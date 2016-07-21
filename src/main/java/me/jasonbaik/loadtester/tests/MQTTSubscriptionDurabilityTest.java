package me.jasonbaik.loadtester.tests;

import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:spring/test/context-test-local-durability.xml" })
public class MQTTSubscriptionDurabilityTest {

	private static final Logger logger = LogManager.getLogger(MQTTSubscriptionDurabilityTest.class);

	private boolean done;

	private int numReceived;
	private int numSent;
	private String topic;
	private String senderId;
	private String sendBroker;
	private String receiverId;
	private String receiveBroker;
	private String receiveReconnectBroker;
	private String brokerUsername;
	private String brokerPassword;
	private Map<String, String> sslProperties;

	private long maxWaitTime;

	@Inject
	private MQTTSubscriptionDurabilityTest mqttSubscriptionDurabilityTest;

	@Test
	public void test() throws Exception {
		mqttSubscriptionDurabilityTest.testDurability();
	}

	private void testDurability() throws Exception {
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(false);
		options.setUserName(brokerUsername);
		options.setPassword(brokerPassword.toCharArray());

		Properties props = new Properties();
		props.putAll(sslProperties);
		options.setSSLProperties(props);

		MemoryPersistence persistenceStore = new MemoryPersistence();

		// Initialize a receiver client that subscribes to the topic topic
		logger.info("Receiver is connecting and creating a durable subscription to the topic " + topic + "...");
		MqttClient receiver = new MqttClient(receiveBroker, receiverId, persistenceStore);
		receiver.connect(options);
		receiver.subscribe(topic); // ActiveMQ MQTT subscriptions are automatically converted to durable JMS subscription

		// Disconnect the receiver client
		logger.info("Disconnecting receiver...");
		receiver.disconnect();

		logger.info("Sleeping 10 seconds");
		Thread.sleep(10 * 1000);

		// Initialize a sender client that sends to the topic topic
		logger.info("Sender is connecting...");
		MqttClient sender = new MqttClient(sendBroker, senderId, new MemoryPersistence());
		sender.connect(options);

		logger.info("Sender is publishing " + numSent + " messages...");

		// Send messages to the topic using the sender client
		for (int i = 1; i <= numSent; i++) {
			sender.publish(topic, Integer.toString(i).getBytes(), 2, false);
		}

		// Reconnect the receiver client. Verify that the message sent by the sender client is received
		logger.info("Reconnecting receiver...");
		MqttClient receiverReconnectClient = new MqttClient(receiveReconnectBroker, receiverId, persistenceStore);
		receiverReconnectClient.setCallback(new MqttCallback() {

			@Override
			public synchronized void messageArrived(String topic, MqttMessage message) throws Exception {
				numReceived++;

				logger.info("Received message #" + new String(message.getPayload()));
				logger.info(numReceived);

				if (numReceived == numSent) {
					synchronized (MQTTSubscriptionDurabilityTest.this) {
						logger.info("Done!");
						done = true;
						MQTTSubscriptionDurabilityTest.this.notifyAll();
					}
				}
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				// TODO Auto-generated method stub

			}

			@Override
			public void connectionLost(Throwable cause) {
				// TODO Auto-generated method stub

			}

		});

		receiverReconnectClient.connect(options);

		long start = System.currentTimeMillis();

		logger.info("Let's wait and see if the messages sent while the receiver was down flows to the receiver when it reconnects...");

		synchronized (this) {
			while (!done) {
				long timeLeft = maxWaitTime - (System.currentTimeMillis() - start);

				if (timeLeft > 0) {
					logger.info("Waiting " + timeLeft);
					this.wait(timeLeft);
				} else {
					break;
				}
			}

			if (done) {
				logger.info("Durable!");
			} else {
				logger.error("Not durable or timed out!");
			}

			System.exit(0);
		}
	}

	public static void main(String[] args) throws BeansException, Exception {
		@SuppressWarnings("resource")
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(args[0]);
		context.getBean(MQTTSubscriptionDurabilityTest.class).testDurability();
	}

	public int getNumSent() {
		return numSent;
	}

	public void setNumSent(int numSent) {
		this.numSent = numSent;
	}

	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

	public String getSendBroker() {
		return sendBroker;
	}

	public void setSendBroker(String sendBroker) {
		this.sendBroker = sendBroker;
	}

	public String getReceiverId() {
		return receiverId;
	}

	public void setReceiverId(String receiverId) {
		this.receiverId = receiverId;
	}

	public String getReceiveBroker() {
		return receiveBroker;
	}

	public void setReceiveBroker(String receiveBroker) {
		this.receiveBroker = receiveBroker;
	}

	public Map<String, String> getSslProperties() {
		return sslProperties;
	}

	public void setSslProperties(Map<String, String> sslProperties) {
		this.sslProperties = sslProperties;
	}

	public long getMaxWaitTime() {
		return maxWaitTime;
	}

	public void setMaxWaitTime(long maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
	}

	public String getBrokerUsername() {
		return brokerUsername;
	}

	public void setBrokerUsername(String brokerUsername) {
		this.brokerUsername = brokerUsername;
	}

	public String getBrokerPassword() {
		return brokerPassword;
	}

	public void setBrokerPassword(String brokerPassword) {
		this.brokerPassword = brokerPassword;
	}

	public MQTTSubscriptionDurabilityTest getMqttSubscriptionDurabilityTest() {
		return mqttSubscriptionDurabilityTest;
	}

	public void setMqttSubscriptionDurabilityTest(MQTTSubscriptionDurabilityTest mqttSubscriptionDurabilityTest) {
		this.mqttSubscriptionDurabilityTest = mqttSubscriptionDurabilityTest;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}

	public String getReceiveReconnectBroker() {
		return receiveReconnectBroker;
	}

	public void setReceiveReconnectBroker(String receiveReconnectBroker) {
		this.receiveReconnectBroker = receiveReconnectBroker;
	}

}
