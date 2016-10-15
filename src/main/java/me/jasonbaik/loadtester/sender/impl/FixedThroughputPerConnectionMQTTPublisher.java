package me.jasonbaik.loadtester.sender.impl;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.ExtendedListener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Topic;

import me.jasonbaik.loadtester.client.MQTTClientFactory;
import me.jasonbaik.loadtester.reporter.impl.ConnectionStatReporter;
import me.jasonbaik.loadtester.reporter.impl.MQTTFlightTracer;
import me.jasonbaik.loadtester.sender.AbstractSender;
import me.jasonbaik.loadtester.util.RandomXmlGenerator;
import me.jasonbaik.loadtester.util.SSLUtil;
import me.jasonbaik.loadtester.valueobject.Broker;
import me.jasonbaik.loadtester.valueobject.MQTTFlightData;
import me.jasonbaik.loadtester.valueobject.Payload;
import me.jasonbaik.loadtester.valueobject.ReportData;

public class FixedThroughputPerConnectionMQTTPublisher extends AbstractSender<byte[], FixedThroughputPerConnectionMQTTPublisherConfig> {

	private static final Logger logger = LogManager.getLogger(FixedThroughputPerConnectionMQTTPublisher.class);

	private final String uuid = UUID.randomUUID().toString();

	private List<byte[]> payloads;

	private AtomicInteger publishedCount = new AtomicInteger(0);
	private AtomicInteger successCount = new AtomicInteger(0);
	private AtomicInteger failureCount = new AtomicInteger(0);
	private AtomicInteger repliedCount = new AtomicInteger(0);

	private AtomicInteger numConnectionsInitiated = new AtomicInteger();
	private AtomicInteger numConnectionsEstablished = new AtomicInteger();
	private AtomicInteger numSubscriptionsInitiated = new AtomicInteger();
	private AtomicInteger numSubscriptionsEstablished = new AtomicInteger();

	private int brokerIndex = 0;

	private ScheduledExecutorService connectionService;
	private volatile long endTimeMillis = Long.MAX_VALUE;

	private volatile List<Pair<String, CallbackConnection>> activeConnections;
	private volatile DelayQueue<DelayedMessage> outboundMessages = new DelayQueue<DelayedMessage>();
	private volatile long messageIntervalMillis;

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

		ConnectCallback(MQTT client, CallbackConnection conn) {
			super();
			this.client = client;
			this.conn = conn;
		}

		@Override
		public void onSuccess(Void value) {
			numConnectionsEstablished.incrementAndGet();
			connectionStatReporter.recordConnectionComp(client.getClientId().toString());

			conn.subscribe(new Topic[] { new Topic(client.getClientId().toString(), getConfig().getQos()) },
					new SubscribeCallback(new Pair<String, CallbackConnection>(client.getClientId().toString(), conn)));
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

		SubscribeCallback(Pair<String, CallbackConnection> conn) {
			this.conn = conn;
		}

		@Override
		public void onSuccess(byte[] value) {
			int subscriptionNum = numSubscriptionsEstablished.incrementAndGet();
			activeConnections.add(conn);
			connectionStatReporter.recordSubscriptionComp(conn.key);

			// Queue the first outbound message for this connection now
			outboundMessages.put(new DelayedMessage(conn, System.currentTimeMillis()));

			if (subscriptionNum == getConfig().getNumConnections()) {
				endTimeMillis = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(getConfig().getDuration(), getConfig().getDurationUnit());
				logger.info("All " + getConfig().getNumConnections() + " subscriptions have been established. The sender will publish the messages for an additional " + getConfig().getDuration() + " "
						+ getConfig().getDurationUnit() + ", then terminate");
				setState("Pub/Sub");
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

	public FixedThroughputPerConnectionMQTTPublisher(FixedThroughputPerConnectionMQTTPublisherConfig config) {
		super(config);
	}

	@Override
	public void init() throws Exception {
		logger.info("Pre-generating a pool of " + getConfig().getMessagePoolSize() + " random payloads...");

		payloads = new ArrayList<byte[]>(getConfig().getMessagePoolSize());

		for (int i = 0; i < getConfig().getMessagePoolSize(); i++) {
			payloads.add(RandomXmlGenerator.generate(getConfig().getMessageByteLength()));
		}

		activeConnections = Collections.synchronizedList(new ArrayList<Pair<String, CallbackConnection>>(getConfig().getNumConnections()));

		if (getConfig().isTrace()) {
			tracers = Collections.synchronizedList(new ArrayList<MQTTFlightTracer>(getConfig().getNumConnections()));
		}

		messageIntervalMillis = TimeUnit.MILLISECONDS.convert(getConfig().getMessageInterval(), getConfig().getMessageIntervalUnit());
	}

	private static class DelayedMessage implements Delayed {

		Pair<String, CallbackConnection> conn;
		long publishTimeMillis;

		DelayedMessage(Pair<String, CallbackConnection> conn, long publishTimeMillis) {
			super();
			this.conn = conn;
			this.publishTimeMillis = publishTimeMillis;
		}

		@Override
		public int compareTo(Delayed o) {
			if (publishTimeMillis < ((DelayedMessage) o).publishTimeMillis) {
				return -1;
			} else if (publishTimeMillis == ((DelayedMessage) o).publishTimeMillis) {
				return 0;
			}
			return 1;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return publishTimeMillis - System.currentTimeMillis();
		}

	}

	@Override
	public void send() throws InterruptedException {
		setState("Conn/Pub/Sub");

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

					if (getConfig().isSsl()) {
						try {
							client.setSslContext(
									SSLUtil.createSSLContext(getConfig().getKeyStore(), getConfig().getKeyStorePassword(), getConfig().getTrustStore(), getConfig().getTrustStorePassword()));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
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

		int index = 0;

		// Stop when the duration expires
		while (endTimeMillis > System.currentTimeMillis()) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Send interrupted");
			}

			DelayedMessage msg = outboundMessages.poll(1, TimeUnit.SECONDS);

			if (msg == null) {
				continue;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Publishing a message using the connection " + msg.conn.key);
			}

			byte[] payload = payloads.get(index % payloads.size());

			msg.conn.value.publish(getConfig().getTopic(), Payload.toBytes(msg.conn.key, Integer.toString(index), payload), getConfig().getQos(), false, publishCallback);
			publishedCount.incrementAndGet();

			// Put the next message for this connection
			outboundMessages.put(new DelayedMessage(msg.conn, System.currentTimeMillis() + messageIntervalMillis));

			index++;
		}

		setState("Sub");
		logger.info("All messages have been published");
		connectionService.shutdown();
	}

	private Broker getNextBroker() {
		return getConfig().getBrokers().get(brokerIndex++ % getConfig().getBrokers().size());
	}

	@Override
	public void destroy() throws Exception {
		outboundMessages.clear();

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
					conn.value.disconnect(null);
				}
			}
		}

		for (MQTTFlightTracer t : tracers) {
			t.destroy();
		}

		tracers.clear();

		if (connectionStatReporter != null) {
			connectionStatReporter.destroy();
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

			reportDatas.add(new ReportData("MQTT_Flight_Data.csv", MQTTFlightTracer.toCsv(flightData)));
		}

		reportDatas.addAll(connectionStatReporter.report());
		return reportDatas;
	}

	public void log() {
		System.out.print(getState());
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