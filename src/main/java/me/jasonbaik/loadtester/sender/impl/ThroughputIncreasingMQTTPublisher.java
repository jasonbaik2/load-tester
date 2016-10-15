package me.jasonbaik.loadtester.sender.impl;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
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
import me.jasonbaik.loadtester.valueobject.KeyValuePair;
import me.jasonbaik.loadtester.valueobject.MQTTFlightData;
import me.jasonbaik.loadtester.valueobject.Payload;
import me.jasonbaik.loadtester.valueobject.ReportData;

public class ThroughputIncreasingMQTTPublisher extends AbstractSender<byte[], ThroughputIncreasingMQTTPublisherConfig> {

	private static final Logger logger = LogManager.getLogger(ThroughputIncreasingMQTTPublisher.class);

	private final String uuid = UUID.randomUUID().toString();

	private List<byte[]> payloads;

	private AtomicInteger publishedCount = new AtomicInteger(0);
	private AtomicInteger successCount = new AtomicInteger(0);
	private AtomicInteger failureCount = new AtomicInteger(0);
	private AtomicInteger repliedCount = new AtomicInteger(0);

	private int brokerIndex = 0;
	private CountDownLatch connectionLatch;
	private volatile ScheduledExecutorService publishService;

	private volatile List<KeyValuePair<String, CallbackConnection>> connections;
	private volatile List<MQTTFlightTracer> tracers;
	private volatile ConnectionStatReporter connectionStatReporter;

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
			connectionStatReporter.recordConnectionComp(client.getClientId().toString());
			conn.subscribe(new Topic[] { new Topic(client.getClientId().toString(), getConfig().getQos()) },
					new SubscribeCallback(new KeyValuePair<String, CallbackConnection>(client.getClientId().toString(), conn)));
			connectionStatReporter.recordSubscriptionInit(client.getClientId().toString());
		}

		@Override
		public void onFailure(Throwable t) {
			logger.error("Connection " + client.getClientId() + " could not be established", t);
		}

	};

	class SubscribeCallback implements Callback<byte[]> {

		KeyValuePair<String, CallbackConnection> conn;

		SubscribeCallback(KeyValuePair<String, CallbackConnection> conn) {
			this.conn = conn;
		}

		@Override
		public void onSuccess(byte[] value) {
			connections.add(conn);
			connectionLatch.countDown();
			connectionStatReporter.recordSubscriptionComp(conn.key);
		}

		@Override
		public void onFailure(Throwable t) {
			logger.error("Subscription faied", t);
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

	public ThroughputIncreasingMQTTPublisher(ThroughputIncreasingMQTTPublisherConfig config) {
		super(config);
	}

	@Override
	public void init() throws Exception {
		logger.info("Pre-generating a pool of " + getConfig().getMessagePoolSize() + " random payloads...");

		payloads = new ArrayList<byte[]>(getConfig().getMessagePoolSize());

		for (int i = 0; i < getConfig().getMessagePoolSize(); i++) {
			payloads.add(RandomXmlGenerator.generate(getConfig().getMessageByteLength()));
		}

		connections = Collections.synchronizedList(new ArrayList<KeyValuePair<String, CallbackConnection>>(getConfig().getNumConnections()));

		if (getConfig().isTrace()) {
			tracers = Collections.synchronizedList(new ArrayList<MQTTFlightTracer>(getConfig().getNumConnections()));
		}

		connectionStatReporter = new ConnectionStatReporter();
	}

	@Override
	public void send() throws InterruptedException {
		connectionLatch = new CountDownLatch(getConfig().getNumConnections());

		for (int i = 0; i < getConfig().getNumConnections(); i++) {
			MQTT client = new MQTT();
			Broker broker = getNextBroker();

			try {
				client.setHost(MQTTClientFactory.getFusesourceConnectionUrl(broker, getConfig().isSsl()));
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}

			String connectionId = uuid + "-" + i;
			client.setClientId(connectionId);
			client.setCleanSession(getConfig().isCleanSession());
			client.setUserName(broker.getUsername());
			client.setPassword(broker.getPassword());
			client.setKeepAlive((short) (getConfig().getKeepAliveIntervalMilli() / 1000));

			if (getConfig().isSsl()) {
				try {
					client.setSslContext(SSLUtil.createSSLContext(getConfig().getKeyStore(), getConfig().getKeyStorePassword(), getConfig().getTrustStore(), getConfig().getTrustStorePassword()));
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

		while (!connectionLatch.await(10, TimeUnit.SECONDS)) {
			interruptIfInterrupted("Interrupted while initializing connections...");
			setState("Connecting " + connectionStatReporter.getNumConnectionsEstablished() + "/" + connectionStatReporter.getNumConnectionsInitiated());
		}

		long throughput = getConfig().getStartThroughput();

		while (throughput < getConfig().getEndThroughput()) {
			long msgIntervalMicro = new BigDecimal(1e6).divide(new BigDecimal(throughput), BigDecimal.ROUND_HALF_EVEN).longValue();
			long endTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(getConfig().getThroughputStepInterval(), getConfig().getThroughputStepIntervalUnit());
			final String threadName = "Pub at " + throughput + " msg/s";

			publishService = Executors.newSingleThreadScheduledExecutor();
			publishService.scheduleAtFixedRate(new FixedThroughputPublishRunnable(publishService, throughput, endTime), 0, msgIntervalMicro, TimeUnit.MICROSECONDS);
			setState(threadName);

			try {
				publishService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				interruptIfInterrupted("Interrupted while sending at " + throughput + " msg/s");
				return;
			}

			throughput += getConfig().getThroughputStepSize();
		}

		setState("Sub");
		logger.info("All messages have been published");
	}

	private class FixedThroughputPublishRunnable implements Runnable {

		private ScheduledExecutorService publishService;
		private long throughput;
		private long endTime;
		private int index;

		FixedThroughputPublishRunnable(ScheduledExecutorService publishService, long throughput, long endTime) {
			this.publishService = publishService;
			this.throughput = throughput;
			this.endTime = endTime;
		}

		@Override
		public void run() {
			if (endTime < System.currentTimeMillis()) {
				publishService.shutdown();
				return;
			}

			try {
				byte[] payload = payloads.get(index % payloads.size());
				KeyValuePair<String, CallbackConnection> conn = connections.get(index % connections.size());
				conn.value.publish(getConfig().getTopic(), Payload.toBytes(conn.key, throughput + "-" + index, payload), getConfig().getQos(), false, publishCallback);
				publishedCount.incrementAndGet();
				index++;

			} catch (Exception e) {
				logger.error("Error", e);
			}
		}

	}

	private Broker getNextBroker() {
		return getConfig().getBrokers().get(brokerIndex++ % getConfig().getBrokers().size());
	}

	@Override
	public void interrupt() throws Exception {
		if (publishService != null) {
			publishService.shutdownNow();
		}
	}

	@Override
	public void destroy() throws Exception {
		interrupt();

		synchronized (connections) {
			for (KeyValuePair<String, CallbackConnection> conn : connections) {
				conn.value.disconnect(null);
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
		System.out.print(connectionStatReporter.getNumConnectionsEstablished());
		System.out.print("/");
		System.out.print(connectionStatReporter.getNumConnectionsInitiated());
		System.out.print(", Subscriptions: ");
		System.out.print(connectionStatReporter.getNumSubscriptionsEstablished());
		System.out.print("/");
		System.out.print(connectionStatReporter.getNumSubscriptionsInitiated());
		System.out.print("\n");
	}

}