package me.jasonbaik.loadtester.sender.impl;

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

import me.jasonbaik.loadtester.client.MQTTClientCallback;
import me.jasonbaik.loadtester.client.MQTTClientFactory;
import me.jasonbaik.loadtester.client.MQTTClientWrapper;
import me.jasonbaik.loadtester.reporter.impl.ConnectionStatReporter;
import me.jasonbaik.loadtester.reporter.impl.MQTTFlightTracer;
import me.jasonbaik.loadtester.sampler.PayloadIterator;
import me.jasonbaik.loadtester.sampler.Sampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;
import me.jasonbaik.loadtester.sender.AbstractSender;
import me.jasonbaik.loadtester.util.RandomXmlGenerator;
import me.jasonbaik.loadtester.valueobject.Broker;
import me.jasonbaik.loadtester.valueobject.MQTTFlightData;
import me.jasonbaik.loadtester.valueobject.Payload;
import me.jasonbaik.loadtester.valueobject.ReportData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

public class ConnectionIncreasingMQTTPublisher extends AbstractSender<byte[], ConnectionIncreasingMQTTPublisherConfig> {

	private static final Logger logger = LogManager.getLogger(ConnectionIncreasingMQTTPublisher.class);

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

	private volatile ScheduledExecutorService connectionService;
	private volatile long endTimeMillis = Long.MAX_VALUE;

	private volatile ArrayBlockingQueue<MQTTClientWrapper> activeConnections;

	private volatile List<MQTTFlightTracer> tracers;
	private volatile ConnectionStatReporter connectionStatReporter = new ConnectionStatReporter();

	class ConnectCallback implements MQTTClientCallback {

		MQTTClientWrapper clientWrapper;

		private ConnectCallback(MQTTClientWrapper clientWrapper) {
			super();
			this.clientWrapper = clientWrapper;
		}

		@Override
		public void onSuccess() throws Exception {
			numConnectionsEstablished.incrementAndGet();
			connectionStatReporter.recordConnectionComp(clientWrapper.getConnectionId());

			try {
				clientWrapper.subscribe(clientWrapper.getConnectionId(), getConfig().getQos(), new SubscribeCallback(clientWrapper));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			numSubscriptionsInitiated.incrementAndGet();
		}

		@Override
		public void onFailure() throws Exception {
			logger.error("Connection " + clientWrapper.getConnectionId() + " could not be established");
			numConnectionsInitiated.decrementAndGet();
		}

	};

	class SubscribeCallback implements MQTTClientCallback {

		MQTTClientWrapper clientWrapper;

		public SubscribeCallback(MQTTClientWrapper clientWrapper) {
			super();
			this.clientWrapper = clientWrapper;
		}

		@Override
		public void onSuccess() throws Exception {
			int subscriptionNum = numSubscriptionsEstablished.incrementAndGet();
			activeConnections.add(clientWrapper);
			connectionStatReporter.recordSubscriptionComp(clientWrapper.getConnectionId());

			if (subscriptionNum == getConfig().getNumConnections()) {
				endTimeMillis = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(getConfig().getDuration(), getConfig().getDurationUnit());
				logger.info("All " + getConfig().getNumConnections() + " subscriptions have been established. The sender will publish the messages for an additional " + getConfig().getDuration()
						+ " " + getConfig().getDurationUnit() + ", then terminate");
				setState("Pub/Sub");
			}
		}

		@Override
		public void onFailure() throws Exception {
			logger.error("Connection " + clientWrapper.getConnectionId() + " could not be established");
			numConnectionsInitiated.decrementAndGet();
		}

	};

	private MQTTClientCallback publishCb = new MQTTClientCallback() {

		@Override
		public void onSuccess() throws Exception {
			successCount.incrementAndGet();
		}

		@Override
		public void onFailure() throws Exception {
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

		activeConnections = new ArrayBlockingQueue<MQTTClientWrapper>(getConfig().getNumConnections());

		if (getConfig().isTrace()) {
			tracers = Collections.synchronizedList(new ArrayList<MQTTFlightTracer>(getConfig().getNumConnections()));
		}
	}

	@Override
	public void send(Sampler<byte[], ?> sampler) throws InterruptedException {
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

						connectionService.shutdownNow();
						return;
					}
				}

				for (int i = 0; i < getConfig().getConnectionStepSize() && i < getConfig().getNumConnections() - numConnectionsInitiated.get(); i++) {
					Broker broker = getNextBroker();

					MQTTClientWrapper clientWrapper = null;
					String connectionId = uuid + "-" + numConnectionsInitiated.getAndIncrement();

					if (MQTTClientFactory.ClientType.FUSESOURCE.equals(getConfig().getClientType())) {
						MQTTFlightTracer tracer = new MQTTFlightTracer();
						tracers.add(tracer);

						clientWrapper = MQTTClientFactory.createFuseSourceMQTTClient(broker, connectionId, getConfig().isSsl(), getConfig().isCleanSession(),
								getConfig().getKeepAliveIntervalMilli() / 1000, tracer);

					} else if (MQTTClientFactory.ClientType.PAHO.equals(getConfig().getClientType())) {
						try {
							clientWrapper = MQTTClientFactory.createPahoMQTTClient(broker, connectionId, getConfig().isSsl(), getConfig().isCleanSession(),
									getConfig().getKeepAliveIntervalMilli() / 1000);
						} catch (MqttException e) {
							logger.error("Failed to initialize a Paho MQTT client", e);
						}
					} else {
						throw new IllegalArgumentException("Unknown MQTT client type");
					}

					clientWrapper.onMessage(new Runnable() {

						@Override
						public void run() {
							repliedCount.incrementAndGet();
						}

					});

					try {
						clientWrapper.connect(new ConnectCallback(clientWrapper));
					} catch (Exception e) {
						logger.error("Exception while connecting", e);
					}

					connectionStatReporter.recordConnectionInit(clientWrapper.getConnectionId());
				}
			}

		}, 0, getConfig().getNewConnectionInterval(), getConfig().getNewConnectionIntervalUnit());

		// Publish messages at a fixed rate using the active connections in a round-robin fashion
		// Stop when the sampler duration expires
		sampler.forEach(new SamplerTask<byte[]>() {

			@Override
			public void run(int index, byte[] payload) throws Exception {
				MQTTClientWrapper clientWrapper = activeConnections.take();

				if (logger.isDebugEnabled()) {
					logger.debug("Publishing a message using the connection " + clientWrapper.getConnectionId());
				}

				clientWrapper.publishAsync(getConfig().getTopic(), Payload.toBytes(clientWrapper.getConnectionId(), Integer.toString(index), payload), getConfig().getQos(), false, publishCb);
				publishedCount.incrementAndGet();

				// Put the connection back into the tail of the blocking queue so it can be reused
				activeConnections.put(clientWrapper);
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

		setState("Sub");
		logger.info("All messages have been published");
		connectionService.shutdown();
	}

	private Broker getNextBroker() {
		return getConfig().getBrokers().get(brokerIndex++ % getConfig().getBrokers().size());
	}

	@Override
	public void destroy() throws Exception {
		if (connectionService != null) {
			connectionService.shutdown();

			while (true) {
				if (connectionService.isShutdown()) {
					break;
				}
				connectionService.awaitTermination(5, TimeUnit.SECONDS);
			}

			synchronized (activeConnections) {
				for (MQTTClientWrapper clientWrapper : activeConnections) {
					clientWrapper.disconnect();
				}
			}
		}

		for (MQTTFlightTracer t : tracers) {
			t.destroy();
		}

		tracers.clear();
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