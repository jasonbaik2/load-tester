package me.jasonbaik.loadtester.tests;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:spring/test/context-test-local-durability.xml" })
public class JMSSubscriptionDurabilityTest {

	private static final Logger logger = LogManager.getLogger(JMSSubscriptionDurabilityTest.class);

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

	private long maxWaitTime;

	@Inject
	private JMSSubscriptionDurabilityTest jmsSubscriptionDurabilityTest;

	@Test
	public void test() throws Exception {
		jmsSubscriptionDurabilityTest.testDurability();
	}

	private void testDurability() throws Exception {
		// Initialize a receiver client that subscribes to the topic topic
		logger.info("Receiver is connecting and creating a durable subscription to the topic " + topic + "...");
		Connection receiverConn = ActiveMQConnection.makeConnection(brokerUsername, brokerPassword, receiveBroker);
		receiverConn.setClientID(receiverId);

		Session receiverSession = receiverConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		receiverSession.createDurableSubscriber(receiverSession.createTopic(topic), receiverId);
		receiverConn.start();

		// Disconnect the receiver client
		logger.info("Disconnecting receiver...");
		receiverConn.close();

		logger.info("Sleeping 10 seconds");
		Thread.sleep(10 * 1000);

		// Initialize a sender client that sends to the topic topic
		logger.info("Sender is connecting...");

		// Initialize a receiver client that subscribes to the topic topic
		logger.info("Receiver is connecting and creating a durable subscription to the topic " + topic + "...");
		Connection senderConn = ActiveMQConnection.makeConnection(brokerUsername, brokerPassword, sendBroker);
		senderConn.setClientID(senderId);

		Session senderSession = senderConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		MessageProducer producer = senderSession.createProducer(senderSession.createTopic(topic));
		senderConn.start();

		logger.info("Sender is publishing " + numSent + " messages...");

		// Send messages to the topic using the sender client
		for (int i = 1; i <= numSent; i++) {
			producer.send(senderSession.createTextMessage(Integer.toString(i)), DeliveryMode.PERSISTENT, 0, Long.MAX_VALUE);
		}

		// Reconnect the receiver client. Verify that the message sent by the sender client is received
		logger.info("Reconnecting receiver...");
		Connection receiverReconnectConn = ActiveMQConnection.makeConnection(brokerUsername, brokerPassword, receiveReconnectBroker);
		receiverReconnectConn.setClientID(receiverId);

		Session receiverReconnectSession = receiverReconnectConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		MessageConsumer consumer = receiverReconnectSession.createDurableSubscriber(receiverReconnectSession.createTopic(topic), receiverId);
		consumer.setMessageListener(new MessageListener() {

			@Override
			public synchronized void onMessage(Message message) {
				numReceived++;

				try {
					logger.info("Received message #" + ((TextMessage) message).getText());
				} catch (JMSException e) {
					logger.error(e);
				}

				logger.info(numReceived);

				if (numReceived == numSent) {
					synchronized (JMSSubscriptionDurabilityTest.this) {
						logger.info("Done!");
						done = true;
						JMSSubscriptionDurabilityTest.this.notifyAll();
					}
				}
			}
		});

		receiverReconnectConn.start();

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
		context.getBean(JMSSubscriptionDurabilityTest.class).testDurability();
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

	public JMSSubscriptionDurabilityTest getJmsSubscriptionDurabilityTest() {
		return jmsSubscriptionDurabilityTest;
	}

	public void setJmsSubscriptionDurabilityTest(JMSSubscriptionDurabilityTest jmsSubscriptionDurabilityTest) {
		this.jmsSubscriptionDurabilityTest = jmsSubscriptionDurabilityTest;
	}

}
