package me.jasonbaik.loadtester.sender.impl;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import me.jasonbaik.loadtester.client.MQTTClientFactory;
import me.jasonbaik.loadtester.sampler.PayloadIterator;
import me.jasonbaik.loadtester.sampler.Sampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;
import me.jasonbaik.loadtester.sender.Sender;
import me.jasonbaik.loadtester.util.MQTTFlightTracer;
import me.jasonbaik.loadtester.util.RandomXmlGenerator;
import me.jasonbaik.loadtester.util.SSLUtil;
import me.jasonbaik.loadtester.valueobject.Broker;
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

public class ConnectionIncreasingMQTTPublisher extends Sender<byte[], ConnectionIncreasingMQTTPublisherConfig> {

	private static final Logger logger = LogManager.getLogger(ConnectionIncreasingMQTTPublisher.class);

	private final String uuid = UUID.randomUUID().toString();

	private List<byte[]> payloads;

	private AtomicInteger publishedCount = new AtomicInteger(0);
	private AtomicInteger successCount = new AtomicInteger(0);
	private AtomicInteger failureCount = new AtomicInteger(0);
	private AtomicInteger repliedCount = new AtomicInteger(0);

	private volatile boolean pubDone;

	private int brokerIndex = 0;

	private volatile int numConnectionsInitiated;
	private AtomicInteger numConnectionsEstablished = new AtomicInteger();
	private AtomicInteger numSubscriptionsEstablished = new AtomicInteger();

	private ScheduledExecutorService connectionService;

	private ArrayBlockingQueue<Pair<String, CallbackConnection>> activeConnections;

	private List<MQTTFlightTracer> tracers;
	private List<Long> connectionEstablishmentTimes;

	static class Pair<K, V> {
		public Pair(K key, V value) {
			super();
			this.key = key;
			this.value = value;
		}

		K key;
		V value;
	}

	class ConnectCallback implements Callback<Void> {

		MQTT client;
		CallbackConnection conn;

		private ConnectCallback(MQTT client, CallbackConnection conn) {
			super();
			this.client = client;
			this.conn = conn;
		}

		@Override
		public void onSuccess(Void value) {
			int connectionNum = numConnectionsEstablished.incrementAndGet();

			logger.info(connectionNum + " / " + numConnectionsInitiated + " connections established so far");

			connectionEstablishmentTimes.add(System.currentTimeMillis());
			conn.subscribe(new Topic[] { new Topic(client.getClientId().toString(), getConfig().getQos()) }, new SubscribeCallback(new Pair<String, CallbackConnection>(uuid + "-" + connectionNum,
					conn)));
		}

		@Override
		public void onFailure(Throwable value) {
			logger.error("Connection " + client.getClientId() + " could not be established", value);
		}

	};

	class SubscribeCallback implements Callback<byte[]> {

		Pair<String, CallbackConnection> conn;

		public SubscribeCallback(Pair<String, CallbackConnection> conn) {
			this.conn = conn;
		}

		@Override
		public void onSuccess(byte[] value) {
			logger.info(numSubscriptionsEstablished.incrementAndGet() + " subscriptions established so far");
			activeConnections.add(conn);
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

			logger.info("Replied: " + repliedCount.incrementAndGet() + " / " + publishedCount.get() + (pubDone ? " (Publish Done)" : ""));
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

	public ConnectionIncreasingMQTTPublisher(ConnectionIncreasingMQTTPublisherConfig config) {
		super(config);
	}

	@Override
	public void init() throws Exception {
		logger.info("Pre-generating a pool of " + getConfig().getMessagePoolSize() + " random payloads...");

		payloads = new ArrayList<byte[]>(getConfig().getMessagePoolSize());

		for (int i = 0; i < getConfig().getMessagePoolSize(); i++) {
			payloads.add(RandomXmlGenerator.generate(getConfig().getMessageByteLength()));
		}

		activeConnections = new ArrayBlockingQueue<Pair<String, CallbackConnection>>(getConfig().getNumConnections());
		connectionEstablishmentTimes = Collections.synchronizedList(new ArrayList<Long>(getConfig().getNumConnections()));

		if (getConfig().isTrace()) {
			tracers = Collections.synchronizedList(new ArrayList<MQTTFlightTracer>(getConfig().getNumConnections()));
		}
	}

	@Override
	public void send(Sampler<byte[], ?> sampler) throws Exception {
		// Start a thread that periodically creates more connections with the broker(s)
		connectionService = Executors.newSingleThreadScheduledExecutor();
		connectionService.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < getConfig().getConnectionStepSize(); i++) {
					MQTT client = new MQTT();
					Broker broker = getNextBroker();

					try {
						client.setHost(MQTTClientFactory.getFusesourceConnectionUrl(broker, getConfig().isSsl()));
					} catch (URISyntaxException e) {
						throw new RuntimeException(e);
					}

					client.setClientId(uuid + "-" + numConnectionsInitiated++);
					client.setCleanSession(getConfig().isCleanSession());
					client.setUserName(broker.getUsername());
					client.setPassword(broker.getPassword());
					client.setKeepAlive((short) (getConfig().getKeepAliveIntervalMilli() / 1000));

					try {
						client.setSslContext(SSLUtil.createSSLContext(getConfig().getKeyStore(), getConfig().getKeyStorePassword(), getConfig().getTrustStore(), getConfig().getTrustStorePassword()));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

					if (getConfig().isTrace()) {
						MQTTFlightTracer tracer = new MQTTFlightTracer();
						client.setTracer(tracer);
						tracers.add(tracer);
					}

					CallbackConnection conn = client.callbackConnection();
					conn.connect(new ConnectCallback(client, conn));
					conn.listener(connectionListener);
				}
			}

		}, 0, getConfig().getNewConnectionInterval(), getConfig().getNewConnectionIntervalUnit());

		// Publish messages at a fixed rate using the active connections in a round-robin fashion
		// Stop when the desired number of connections is reached, or the sampler duration expires
		sampler.forEach(new SamplerTask<byte[]>() {

			@Override
			public void run(int index, byte[] payload) throws Exception {
				Pair<String, CallbackConnection> conn = activeConnections.take();

				logger.debug("Publishing a message using the connection " + conn.key);

				conn.value.publish(getConfig().getTopic(), Payload.toBytes(conn.key, index, payload), getConfig().getQos(), false, publishCallback);
				publishedCount.incrementAndGet();

				// Put the connection back into the tail of the blocking queue so it can be reused
				activeConnections.put(conn);
			}

		}, new PayloadIterator<byte[]>() {

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
				if (numConnectionsEstablished.get() < getConfig().getNumConnections()) {
					return true;
				}
				return false;
			}

		});

		pubDone = true;
		connectionService.shutdown();
	}

	private Broker getNextBroker() {
		return getConfig().getBrokers().get(brokerIndex++ % getConfig().getBrokers().size());
	}

	@Override
	public void destroy() throws Exception {
		connectionService.shutdown();

		while (true) {
			if (connectionService.isShutdown()) {
				break;
			}
			connectionService.awaitTermination(5, TimeUnit.SECONDS);
		}

		for (Pair<String, CallbackConnection> conn : activeConnections) {
			conn.value.disconnect(null);
		}

		tracers.clear();
	}

	@Override
	public ArrayList<ReportData> report() throws InterruptedException {
		ArrayList<ReportData> reportDatas = new ArrayList<ReportData>();

		if (getConfig().isTrace()) {
			List<MQTTFlightData> flightData = new LinkedList<MQTTFlightData>();

			for (MQTTFlightTracer tracer : this.tracers) {
				flightData.addAll(tracer.getFlightData());
			}

			reportDatas.add(new ReportData("RoundRobinMQTTPublisher_MQTT_Flight_Data.csv", MQTTFlightTracer.toCsv(flightData)));
		}

		StringBuilder sb = new StringBuilder("Connection_Establishment_Times\n");

		for (Long l : connectionEstablishmentTimes) {
			sb.append(Long.toString(l)).append("\n");
		}

		reportDatas.add(new ReportData("Connection_Establishment_Times.csv", sb.toString().getBytes()));

		return reportDatas;
	}

}