package me.jasonbaik.loadtester.sender.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
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
import me.jasonbaik.loadtester.util.RandomXmlGenerator;
import me.jasonbaik.loadtester.valueobject.Payload;
import me.jasonbaik.loadtester.valueobject.ReportData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class RoundRobinPahoMQTTPublisher extends Sender<byte[], RoundRobinPahoMQTTPublisherConfig> {

	private static final Logger logger = LogManager.getLogger(RoundRobinPahoMQTTPublisher.class);

	private final String uuid = UUID.randomUUID().toString();

	private List<MqttAsyncClient> clients;

	private AtomicInteger clientIndex = new AtomicInteger(0);
	private AtomicInteger seqNum = new AtomicInteger(0);
	private AtomicInteger publishedCount = new AtomicInteger(0);
	private AtomicInteger completedCount = new AtomicInteger(0);
	private AtomicInteger disconnectedWhileInFlightCount = new AtomicInteger(0);
	private AtomicInteger repliedCount = new AtomicInteger(0);

	private List<Integer> publishedSeqNums = Collections.synchronizedList(new ArrayList<Integer>());
	private List<Integer> completedSeqNums = Collections.synchronizedList(new ArrayList<Integer>());
	private List<Integer> disconnectedInFlightSeqNums = Collections.synchronizedList(new ArrayList<Integer>());
	private List<Integer> repliedSeqNums = Collections.synchronizedList(new ArrayList<Integer>());
	private List<String> topicMismatchedMsgs = new LinkedList<String>();

	private CountDownLatch connectionLatch;

	private List<byte[]> payloads;

	public RoundRobinPahoMQTTPublisher(RoundRobinPahoMQTTPublisherConfig config) {
		super(config);
	}

	private class ConnectCallback implements IMqttActionListener {

		private MqttAsyncClient client;
		private boolean subscribing;

		private ConnectCallback(MqttAsyncClient client) {
			super();
			this.client = client;
		}

		@Override
		public synchronized void onSuccess(IMqttToken asyncActionToken) {
			if (!subscribing) {
				logger.info(client.getClientId() + " connected successfully");

				try {
					client.subscribe(client.getClientId().toString(), getConfig().getQos(), null, this);
					subscribing = true;

				} catch (MqttException e) {
					logger.error("Failed to subscribe", e);
				}
			} else {
				logger.info(client.getClientId() + " subscribed successfully");
				connectionLatch.countDown();
			}
		}

		@Override
		public synchronized void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			if (!subscribing) {
				logger.error("Failed to establish connection", exception);
				subscribing = false;

			} else {
				logger.error("Failed to subscribe", exception);
			}
		}

	};

	private class MessageCallback implements MqttCallback {

		private MqttAsyncClient client;
		private MqttConnectOptions connectOptions;
		private ConnectCallback connectCallback;

		public MessageCallback(MqttAsyncClient client, MqttConnectOptions connectOptions, ConnectCallback connectCallback) {
			super();
			this.client = client;
			this.connectOptions = connectOptions;
			this.connectCallback = connectCallback;
		}

		@Override
		public void connectionLost(Throwable cause) {
			logger.error("Connection lost", cause);

			try {
				String[] rotatedBrokers = rotateBrokers(this.connectOptions.getServerURIs());
				logger.info("Reconnecting to: " + rotatedBrokers[0]);
				this.connectOptions.setServerURIs(rotatedBrokers);
				this.client.connect(this.connectOptions, null, connectCallback);

			} catch (MqttSecurityException e) {
				logger.error(e);
			} catch (MqttException e) {
				logger.error(e);
			}
		}

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			if (logger.isDebugEnabled()) {
				logger.debug("Received a reply on connection=" + topic + " for message id=" + Payload.extractMessageId(message.getPayload()));
			}

			if (!topic.equals(Payload.extractConnectionId(message.getPayload()))) {
				logger.error("Received a message sent from the connection: " + Payload.extractConnectionId(message.getPayload()) + " to " + topic);

				synchronized (topicMismatchedMsgs) {
					topicMismatchedMsgs.add(Payload.extractUniqueId(message.getPayload()));
				}
			}

			logger.info("Replied: " + repliedCount.incrementAndGet());
			repliedSeqNums.add(Integer.parseInt(Payload.extractMessageId(message.getPayload())));
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
			completedCount.incrementAndGet();
			printCounts();

			if (token.getUserContext() != null) {
				completedSeqNums.add((Integer) token.getUserContext());
			}
		}

	};

	private IMqttActionListener publishCallback = new IMqttActionListener() {

		@Override
		public void onSuccess(IMqttToken asyncActionToken) {
		}

		@Override
		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			logger.error(exception);
			disconnectedWhileInFlightCount.incrementAndGet();
			printCounts();

			if (asyncActionToken.getUserContext() != null) {
				disconnectedInFlightSeqNums.add((Integer) asyncActionToken.getUserContext());
			}
		}

	};

	private void printCounts() {
		logger.info("Published: " + publishedCount.get() + ", " + "Completed: " + completedCount.get() + ", Disconnected While in Flight: " + disconnectedWhileInFlightCount.get());
	}

	@Override
	public void init() throws MqttException, InterruptedException, JAXBException {
		clients = Collections.synchronizedList(new ArrayList<MqttAsyncClient>(getConfig().getNumConnections()));
		connectionLatch = new CountDownLatch(getConfig().getNumConnections());

		logger.info("Initiating " + getConfig().getNumConnections() + " connections with a step size of " + getConfig().getConnectionStepSize() + " and a step interval of "
				+ getConfig().getConnectionStepIntervalMilli() + "ms...");

		String[] brokers = getConfig().getMqttBrokers();

		for (int i = 0; i < getConfig().getNumConnections(); i++) {
			MqttAsyncClient mqttClient = new MqttAsyncClient(getNextBroker(), uuid + "-" + i, new MemoryPersistence());

			MqttConnectOptions options = new MqttConnectOptions();

			if (getConfig().getMqttBrokers() != null) {
				brokers = rotateBrokers(brokers);
				options.setServerURIs(brokers);
			}

			options.setCleanSession(getConfig().isCleanSession());
			options.setUserName(getConfig().getMqttBrokerUsername());
			options.setPassword(getConfig().getMqttBrokerPassword().toCharArray());
			options.setKeepAliveInterval((int) (getConfig().getKeepAliveIntervalMilli() / 1000));

			Properties props = new Properties();
			props.putAll(getConfig().getSslProperties());
			options.setSSLProperties(props);

			ConnectCallback connectCallback = new ConnectCallback(mqttClient);
			mqttClient.setCallback(new MessageCallback(mqttClient, options, connectCallback));
			mqttClient.connect(options, null, connectCallback);
			clients.add(mqttClient);

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

	static String[] rotateBrokers(String[] brokers) {
		String[] rotated = new String[brokers.length];
		System.arraycopy(brokers, 1, rotated, 0, brokers.length - 1);
		rotated[brokers.length - 1] = brokers[0];
		return rotated;
	}

	@PreDestroy
	@Override
	public void destroy() throws MqttException {
		for (MqttAsyncClient client : clients) {
			client.disconnect();
		}

		clients.clear();
	}

	@Override
	public void send(Sampler<byte[], ?> sampler) throws Exception {
		SamplerTask<byte[]> task = new SamplerTask<byte[]>() {

			@Override
			public void run(int index, byte[] payload) throws Exception {
				int cIndex = clientIndex.getAndIncrement() % clients.size();
				String connectionId = uuid + "-" + (cIndex);

				logger.debug("Publishing a message using the client #" + cIndex);
				clients.get(cIndex).publish(getConfig().getTopic(), Payload.toBytes(connectionId, index, payload), getConfig().getQos(), false, index, publishCallback);
				publishedCount.incrementAndGet();
				printCounts();
				publishedSeqNums.add(index);
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
					return payloads.get(seqNum.getAndIncrement() % payloads.size());
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
					return payloads.get(seqNum.getAndIncrement() % payloads.size());
				}

				@Override
				public boolean hasNext() {
					if (seqNum.get() == getConfig().getNumMessages()) {
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
		ArrayList<ReportData> reports = new ArrayList<ReportData>();

		StringBuilder sb = new StringBuilder();

		for (String m : topicMismatchedMsgs) {
			sb.append(m).append("\n");
		}

		reports.add(new ReportData("Topic_Mismatched_Messages.csv", sb.toString().getBytes()));

		sb = new StringBuilder();

		for (Integer i : publishedSeqNums) {
			sb.append(i).append("\n");
		}

		reports.add(new ReportData("Published_Seq_Nums.csv", sb.toString().getBytes()));

		sb = new StringBuilder();

		for (Integer i : completedSeqNums) {
			sb.append(i).append("\n");
		}

		reports.add(new ReportData("Completed_Seq_Nums.csv", sb.toString().getBytes()));

		sb = new StringBuilder();

		for (Integer i : disconnectedInFlightSeqNums) {
			sb.append(i).append("\n");
		}

		reports.add(new ReportData("Disconnected_Seq_Nums.csv", sb.toString().getBytes()));

		sb = new StringBuilder();

		for (Integer i : repliedSeqNums) {
			sb.append(i).append("\n");
		}

		reports.add(new ReportData("Replied_Seq_Nums.csv", sb.toString().getBytes()));

		return reports;
	}

}