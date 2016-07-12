package me.jasonbaik.loadtester.sender.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBException;

import me.jasonbaik.loadtester.sampler.PayloadIterator;
import me.jasonbaik.loadtester.sampler.Sampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;
import me.jasonbaik.loadtester.sender.Sender;
import me.jasonbaik.loadtester.util.MQTTFlightTracer;
import me.jasonbaik.loadtester.util.RandomXmlGenerator;
import me.jasonbaik.loadtester.util.SSLUtil;
import me.jasonbaik.loadtester.valueobject.MQTTFlightData;
import me.jasonbaik.loadtester.valueobject.Payload;
import me.jasonbaik.loadtester.valueobject.ReportData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.ExtendedListener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Topic;

public class RoundRobinMQTTPublisher extends Sender<byte[], RoundRobinMQTTPublisherConfig> {

	private static final Logger logger = LogManager.getLogger(RoundRobinMQTTPublisher.class);

	private final String uuid = UUID.randomUUID().toString();

	private List<CallbackConnection> connections;
	private List<MQTTFlightTracer> tracers;

	private AtomicInteger clientIndex = new AtomicInteger(0);
	private AtomicInteger publishedCount = new AtomicInteger(0);
	private AtomicInteger successCount = new AtomicInteger(0);
	private AtomicInteger failureCount = new AtomicInteger(0);
	private AtomicInteger repliedCount = new AtomicInteger(0);

	private List<String> topicMismatchedMsgs = new LinkedList<String>();

	private CountDownLatch connectionLatch;

	private class ConnectCallback implements Callback<Void> {

		private MQTT client;
		private CallbackConnection conn;

		private ConnectCallback(MQTT client, CallbackConnection conn) {
			super();
			this.client = client;
			this.conn = conn;
		}

		@Override
		public void onSuccess(Void value) {
			conn.subscribe(new Topic[] { new Topic(client.getClientId().toString(), getConfig().getQos()) }, subscribeCallback);
		}

		@Override
		public void onFailure(Throwable value) {
			logger.error(value);
		}

	};

	private Callback<byte[]> subscribeCallback = new Callback<byte[]>() {

		@Override
		public void onSuccess(byte[] value) {
			connectionLatch.countDown();
		}

		@Override
		public void onFailure(Throwable value) {
			logger.error(value);
		}

	};

	private ExtendedListener connectionListener = new ExtendedListener() {

		@Override
		public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
			log(topic, body);
			ack.run();
		}

		@Override
		public void onFailure(Throwable value) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onDisconnected() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onConnected() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onPublish(UTF8Buffer topic, Buffer body, Callback<Callback<Void>> ack) {
			log(topic, body);
			ack.onSuccess(null);
		}

		private void log(UTF8Buffer topic, Buffer body) {
			if (logger.isDebugEnabled()) {
				logger.debug("Received a reply on connection=" + topic + " for message id=" + Payload.extractMessageId(body.toByteArray()));
			}

			if (!topic.toString().equals(Payload.extractConnectionId(body.toByteArray()))) {
				logger.error("Received a message sent from the connection: " + Payload.extractConnectionId(body.toByteArray()) + " to " + topic);

				synchronized (topicMismatchedMsgs) {
					topicMismatchedMsgs.add(Payload.extractUniqueId(body.toByteArray()));
				}
			}

			logger.info("Replied: " + repliedCount.incrementAndGet());
		}

	};

	private Callback<Void> publishCallback = new Callback<Void>() {

		@Override
		public void onSuccess(Void value) {
			printCounts(true);
		}

		@Override
		public void onFailure(Throwable value) {
			logger.error(value);
			printCounts(false);
		}

		private void printCounts(boolean success) {
			int sCount;
			int fCount;

			if (success) {
				sCount = successCount.incrementAndGet();
				fCount = failureCount.get();
			} else {
				sCount = successCount.get();
				fCount = failureCount.incrementAndGet();
			}

			logger.info("Published: " + publishedCount.get() + ", " + "Success: " + sCount + ", Failed: " + fCount);
		}

	};

	private List<byte[]> payloads;

	public RoundRobinMQTTPublisher(RoundRobinMQTTPublisherConfig config) {
		super(config);
	}

	@Override
	public void init() throws URISyntaxException, InterruptedException, JAXBException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, CertificateException,
			FileNotFoundException, KeyStoreException, IOException {
		connections = Collections.synchronizedList(new ArrayList<CallbackConnection>(getConfig().getNumConnections()));
		tracers = Collections.synchronizedList(new ArrayList<MQTTFlightTracer>(getConfig().getNumConnections()));
		connectionLatch = new CountDownLatch(getConfig().getNumConnections());

		logger.info("Initiating " + getConfig().getNumConnections() + " connections with a step size of " + getConfig().getConnectionStepSize() + " and a step interval of "
				+ getConfig().getConnectionStepIntervalMilli() + "ms...");

		for (int i = 0; i < getConfig().getNumConnections(); i++) {
			MQTT client = new MQTT();
			client.setHost(getNextBroker());
			client.setClientId(uuid + "-" + i);
			client.setCleanSession(getConfig().isCleanSession());
			client.setUserName(getConfig().getMqttBrokerUsername());
			client.setPassword(getConfig().getMqttBrokerPassword());
			client.setKeepAlive((short) (getConfig().getKeepAliveIntervalMilli() / 1000));
			client.setSslContext(SSLUtil.createSSLContext(getConfig().getKeyStore(), getConfig().getKeyStorePassword(), getConfig().getTrustStore(), getConfig().getTrustStorePassword()));

			MQTTFlightTracer tracer = new MQTTFlightTracer();
			client.setTracer(tracer);
			tracers.add(tracer);

			CallbackConnection conn = client.callbackConnection();
			conn.connect(new ConnectCallback(client, conn));
			conn.listener(connectionListener);

			connections.add(conn);

			if (i != 0 && i % getConfig().getConnectionStepSize() == 0) {
				logger.info("Sleeping for " + getConfig().getConnectionStepIntervalMilli() + "ms to allow the last " + getConfig().getConnectionStepSize() + " connections to be established");
				Thread.sleep(getConfig().getConnectionStepIntervalMilli());
			}
		}

		while (!connectionLatch.await(10, TimeUnit.SECONDS)) {
			logger.info(connectionLatch.getCount() + " connections are still yet to be established");
			logger.info("Waiting another 10 seconds until all connections are established");
		}

		logger.info("Successfully established all " + getConfig().getNumConnections() + " connections");

		logger.info("Pre-generating a pool of " + getConfig().getMessagePoolSize() + " random payloads...");

		payloads = new ArrayList<byte[]>(getConfig().getMessagePoolSize());

		for (int i = 0; i < getConfig().getMessagePoolSize(); i++) {
			payloads.add(RandomXmlGenerator.generate(getConfig().getMessageByteLength()));
		}
	}

	private int brokerIndex = 0;

	private String getNextBroker() {
		if (getConfig().getMqttBrokers() != null) {
			return getConfig().getMqttBrokers()[brokerIndex++ % getConfig().getMqttBrokers().length];
		}
		return getConfig().getMqttBroker();
	}

	@PreDestroy
	@Override
	public void destroy() {
		for (CallbackConnection conn : connections) {
			conn.disconnect(null);
		}

		connections.clear();
		tracers.clear();
	}

	@Override
	public void send(Sampler<byte[], ?> sampler) throws Exception {
		SamplerTask<byte[]> task = new SamplerTask<byte[]>() {

			@Override
			public void run(int index, byte[] payload) throws Exception {
				int cIndex = clientIndex.getAndIncrement() % connections.size();
				String connectionId = uuid + "-" + (cIndex);

				logger.debug("Publishing a message using the client #" + cIndex);
				connections.get(cIndex).publish(getConfig().getTopic(), Payload.toBytes(connectionId, publishedCount.getAndIncrement(), payload), getConfig().getQos(), false, publishCallback);
			}

		};

		if (getConfig().getDuration() != null) {
			logger.info("Publishing messages for " + getConfig().getDuration() + " " + getConfig().getDurationUnit());

			PayloadIterator<byte[]> payloadIterator = new PayloadIterator<byte[]>() {

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public byte[] next() {
					return payloads.get(publishedCount.get() % payloads.size());
				}

				@Override
				public boolean hasNext() {
					return true;
				}

			};

			sampler.during(task, payloadIterator, getConfig().getDuration(), getConfig().getDurationUnit());

		} else if (getConfig().getNumMessages() != null) {
			logger.info("Publishing " + getConfig().getNumMessages() + " messages...");

			PayloadIterator<byte[]> payloadIterator = new PayloadIterator<byte[]>() {

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public byte[] next() {
					return payloads.get(publishedCount.get() % payloads.size());
				}

				@Override
				public boolean hasNext() {
					if (publishedCount.get() == getConfig().getNumMessages()) {
						return false;
					}
					return true;
				}

			};

			sampler.forEach(task, payloadIterator);
		}
	}

	@Override
	public ArrayList<ReportData> report() {
		List<MQTTFlightData> flightData = new LinkedList<MQTTFlightData>();

		for (MQTTFlightTracer tracer : this.tracers) {
			flightData.addAll(tracer.getFlightData());
		}

		StringBuilder sb = new StringBuilder();

		for (String m : topicMismatchedMsgs) {
			sb.append(m).append("\n");
		}

		return new ArrayList<ReportData>(Arrays.asList(new ReportData[] { new ReportData("RoundRobinMQTTPublisher_MQTT_Flight_Data.csv", MQTTFlightTracer.toCsv(flightData)),
				new ReportData("Topic_Mismatched_Messages.csv", sb.toString().getBytes()) }));
	}

}