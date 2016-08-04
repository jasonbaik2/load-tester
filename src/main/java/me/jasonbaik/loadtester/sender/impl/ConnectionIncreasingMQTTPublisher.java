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
import me.jasonbaik.loadtester.reporter.impl.ConnectionStatReporter;
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

	private volatile String state = "Conn/Pub/Sub";

	private AtomicInteger numConnectionsInitiated = new AtomicInteger();
	private AtomicInteger numConnectionsEstablished = new AtomicInteger();
	private AtomicInteger numSubscriptionsInitiated = new AtomicInteger();
	private AtomicInteger numSubscriptionsEstablished = new AtomicInteger();

	private int brokerIndex = 0;

	private ScheduledExecutorService statLoggingService;
	private ScheduledExecutorService connectionService;
	private volatile long endTimeMillis = Long.MAX_VALUE;

	private ArrayBlockingQueue<Pair<String, CallbackConnection>> activeConnections;

	private List<MQTTFlightTracer> tracers;
	private ConnectionStatReporter connectionStatReporter = new ConnectionStatReporter();

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
			numConnectionsEstablished.incrementAndGet();
			connectionStatReporter.recordConnectionComp(client.getClientId().toString());

			conn.subscribe(new Topic[] { new Topic(client.getClientId().toString(), getConfig().getQos()) }, new SubscribeCallback(new Pair<String, CallbackConnection>(
					client.getClientId().toString(), conn)));
			numSubscriptionsInitiated.incrementAndGet();
		}

		@Override
		public void onFailure(Throwable value) {
			logger.error("Connection " + client.getClientId() + " could not be established", value);
			numConnectionsInitiated.decrementAndGet();
		}

	};

	class SubscribeCallback implements Callback<byte[]> {

		Pair<String, CallbackConnection> conn;

		public SubscribeCallback(Pair<String, CallbackConnection> conn) {
			this.conn = conn;
		}

		@Override
		public void onSuccess(byte[] value) {
			int subscriptionNum = numSubscriptionsEstablished.incrementAndGet();
			activeConnections.add(conn);
			connectionStatReporter.recordSubscriptionComp(conn.key);

			if (subscriptionNum == getConfig().getNumConnections()) {
				endTimeMillis = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(getConfig().getDuration(), getConfig().getDurationUnit());
				logger.info("All " + getConfig().getNumConnections() + " subscriptions have been established. The sender will publish the messages for an additional " + getConfig().getDuration()
						+ " " + getConfig().getDurationUnit() + ", then terminate");
				state = "Pub/Sub";
			}
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

			repliedCount.incrementAndGet();
		}

	};

	private Callback<Void> publishCallback = new Callback<Void>() {

		@Override
		public void onSuccess(Void value) {
			successCount.incrementAndGet();
		}

		@Override
		public void onFailure(Throwable value) {
			failureCount.incrementAndGet();
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

		if (getConfig().isTrace()) {
			tracers = Collections.synchronizedList(new ArrayList<MQTTFlightTracer>(getConfig().getNumConnections()));
		}
	}

	@Override
	public void send(Sampler<byte[], ?> sampler) throws InterruptedException {
		// Start a thread that periodically creates more connections with the broker(s)
		connectionService = Executors.newSingleThreadScheduledExecutor();
		connectionService.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				if (numConnectionsInitiated.get() >= getConfig().getNumConnections()) {
					synchronized (connectionService) {
						if (connectionService.isShutdown()) {
							return;
						}

						connectionService.shutdown();
						return;
					}
				}

				for (int i = 0; i < getConfig().getConnectionStepSize(); i++) {
					MQTT client = new MQTT();
					Broker broker = getNextBroker();

					try {
						client.setHost(MQTTClientFactory.getFusesourceConnectionUrl(broker, getConfig().isSsl()));
					} catch (URISyntaxException e) {
						throw new RuntimeException(e);
					}

					String connectionId = uuid + "-" + numConnectionsInitiated.getAndIncrement();
					client.setClientId(connectionId);
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
					conn.listener(connectionListener);
					conn.connect(new ConnectCallback(client, conn));
					connectionStatReporter.recordConnectionInit(connectionId);
				}
			}

		}, 0, getConfig().getNewConnectionInterval(), getConfig().getNewConnectionIntervalUnit());

		// Publish messages at a fixed rate using the active connections in a round-robin fashion
		// Stop when the sampler duration expires
		sampler.forEach(new SamplerTask<byte[]>() {

			@Override
			public void run(int index, byte[] payload) throws Exception {
				Pair<String, CallbackConnection> conn = activeConnections.take();

				if (logger.isDebugEnabled()) {
					logger.debug("Publishing a message using the connection " + conn.key);
				}

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
				if (endTimeMillis < System.currentTimeMillis()) {
					return false;
				}
				return true;
			}

		});

		state = "Sub";
		logger.info("All messages have been published");
		connectionService.shutdown();
	}

	private Broker getNextBroker() {
		return getConfig().getBrokers().get(brokerIndex++ % getConfig().getBrokers().size());
	}

	@Override
	public void destroy() throws Exception {
		if (statLoggingService != null) {
			statLoggingService.shutdown();
		}

		if (connectionService != null) {
			connectionService.shutdown();

			while (true) {
				if (connectionService.isShutdown()) {
					break;
				}
				connectionService.awaitTermination(5, TimeUnit.SECONDS);
			}

			synchronized (activeConnections) {
				for (Pair<String, CallbackConnection> conn : activeConnections) {
					conn.value.kill(null);
				}
			}
		}

		if (tracers != null) {
			tracers.clear();
		}
	}

	@Override
	public ArrayList<ReportData> report() throws InterruptedException {
		ArrayList<ReportData> reportDatas = new ArrayList<ReportData>();

		if (getConfig().isTrace()) {
			List<MQTTFlightData> flightData = new LinkedList<MQTTFlightData>();

			synchronized (this.tracers) {
				for (MQTTFlightTracer tracer : this.tracers) {
					flightData.addAll(tracer.getFlightData());
				}
			}

			reportDatas.add(new ReportData("RoundRobinMQTTPublisher_MQTT_Flight_Data.csv", MQTTFlightTracer.toCsv(flightData)));
		}

		reportDatas.addAll(connectionStatReporter.report());
		return reportDatas;
	}

	public void log() {
		System.out.print(state);
		System.out.print("\tPublished: ");
		System.out.print(publishedCount);
		System.out.print(", Replied: ");
		System.out.print(repliedCount);
		System.out.print(", Success: ");
		System.out.print(successCount);
		System.out.print(", Failed: ");
		System.out.print(failureCount);
		System.out.print(", Connections: ");
		System.out.print(numConnectionsEstablished);
		System.out.print("/");
		System.out.print(numConnectionsInitiated);
		System.out.print(", Subscriptions: ");
		System.out.print(numSubscriptionsEstablished);
		System.out.print("/");
		System.out.print(numSubscriptionsInitiated);
		System.out.print("\n");
	}

}