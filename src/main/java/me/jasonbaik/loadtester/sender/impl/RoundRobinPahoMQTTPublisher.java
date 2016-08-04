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

import me.jasonbaik.loadtester.client.MQTTClientFactory;
import me.jasonbaik.loadtester.valueobject.Broker;
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
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class RoundRobinPahoMQTTPublisher extends AbstractRoundRobinMQTTPublisher<RoundRobinPahoMQTTPublisherConfig> {

	private static final Logger logger = LogManager.getLogger(RoundRobinPahoMQTTPublisher.class);

	private final String uuid = UUID.randomUUID().toString();

	private List<MqttAsyncClient> clients;
	private CountDownLatch connectionLatch;
	private List<String> topicMismatchedMsgs = new LinkedList<String>();

	private AtomicInteger disconnectedWhileInFlightCount = new AtomicInteger(0);

	private List<Integer> publishedSeqNums = Collections.synchronizedList(new ArrayList<Integer>());
	private List<Integer> completedSeqNums = Collections.synchronizedList(new ArrayList<Integer>());
	private List<Integer> disconnectedInFlightSeqNums = Collections.synchronizedList(new ArrayList<Integer>());
	private List<Integer> repliedSeqNums = Collections.synchronizedList(new ArrayList<Integer>());

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

			repliedSeqNums.add(Integer.parseInt(Payload.extractMessageId(message.getPayload())));
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
			getSuccessCount().incrementAndGet();

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

			if (asyncActionToken.getUserContext() != null) {
				disconnectedInFlightSeqNums.add((Integer) asyncActionToken.getUserContext());
			}
		}

	};

	public RoundRobinPahoMQTTPublisher(RoundRobinPahoMQTTPublisherConfig config) {
		super(config);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init() throws Exception {
		super.init();

		clients = Collections.synchronizedList(new ArrayList<MqttAsyncClient>(getConfig().getNumConnections()));
		connectionLatch = new CountDownLatch(getConfig().getNumConnections());
	}

	static String[] rotateBrokers(String[] brokers) {
		String[] rotated = new String[brokers.length];
		System.arraycopy(brokers, 1, rotated, 0, brokers.length - 1);
		rotated[brokers.length - 1] = brokers[0];
		return rotated;
	}

	@Override
	protected void connect() throws Exception {
		logger.info("Initiating " + getConfig().getNumConnections() + " connections with a step size of " + getConfig().getConnectionStepSize() + " and a step interval of "
				+ getConfig().getConnectionStepIntervalMilli() + "ms...");

		List<Broker> brokers = getConfig().getBrokers();
		String[] brokerUrls = new String[brokers.size()];

		for (int i = 0; i < brokers.size(); i++) {
			brokerUrls[i] = MQTTClientFactory.getPahoConnectionUrl(brokers.get(i), getConfig().isSsl());
		}

		for (int i = 0; i < getConfig().getNumConnections(); i++) {
			Broker broker = getNextBroker();
			MqttAsyncClient mqttClient = new MqttAsyncClient(MQTTClientFactory.getPahoConnectionUrl(broker, getConfig().isSsl()), uuid + "-" + i, new MemoryPersistence());

			MqttConnectOptions options = new MqttConnectOptions();

			if (getConfig().getBrokers().size() > 0) {
				brokerUrls = rotateBrokers(brokerUrls);
				options.setServerURIs(brokerUrls);
			}

			options.setCleanSession(getConfig().isCleanSession());
			options.setUserName(broker.getUsername());
			options.setPassword(broker.getPassword().toCharArray());
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
	};

	@Override
	protected void roundRobinSend(int index, byte[] payload) throws MqttPersistenceException, MqttException {
		int cIndex = getClientIndex().getAndIncrement() % clients.size();
		String connectionId = uuid + "-" + (cIndex);

		logger.debug("Publishing a message using the client #" + cIndex);
		clients.get(cIndex).publish(getConfig().getTopic(), Payload.toBytes(connectionId, index, payload), getConfig().getQos(), false, index, publishCallback);
		getPublishedCount().incrementAndGet();
		publishedSeqNums.add(index);
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

	@Override
	public void destroy() throws MqttException {
		for (MqttAsyncClient client : clients) {
			client.disconnectForcibly();
		}

		clients.clear();
	}

	@Override
	public void log() {
		System.out.print(getState());
		System.out.print("\tPublished: ");
		System.out.print(getPublishedCount());
		System.out.print(", Replied: ");
		System.out.print(getRepliedCount());
		System.out.print(", Completed: ");
		System.out.print(getSuccessCount());
		System.out.print(", Disconnected While in Flight: ");
		System.out.print(disconnectedWhileInFlightCount);
		System.out.print("\n");
	}

}