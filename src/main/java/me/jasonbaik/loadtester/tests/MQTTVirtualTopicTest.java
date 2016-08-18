package me.jasonbaik.loadtester.tests;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class MQTTVirtualTopicTest {

	private static final String VIRTUAL_TOPIC = "VirtualTopic";

	private static Connection conn;
	private static Session session;

	@BeforeClass
	public static void init() throws Exception {
		// BrokerService broker = new BrokerService();
		//
		// TransportConnector mqttConnector = new TransportConnector();
		// mqttConnector.setUri(new URI("mqtt://localhost:1883"));
		// broker.addConnector(mqttConnector);
		//
		// TransportConnector jmsConnector = new TransportConnector();
		// jmsConnector.setUri(new URI("tcp://localhost:61616"));
		// broker.addConnector(jmsConnector);
		//
		// broker.start();

		conn = ActiveMQConnection.makeConnection("admin", "admin", "tcp://localhost:61616");
		conn.setClientID("test.jms.conn");
		session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		conn.start();
	}

	@AfterClass
	public static void destroy() throws JMSException {
		conn.close();
	}

	@Test
	@Ignore
	public void testVirtualTopicWithMQTT() throws Exception {
		String consumerId = "test.jms.consumer.mqtt";
		MessageConsumer consumer = session.createDurableSubscriber(session.createTopic("VirtualTopic.test.topic"), consumerId);
		consumer.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(Message message) {
				System.out.println(message);
			}

		});

		MqttConnectOptions options = new MqttConnectOptions();
		options.setUserName("admin");
		options.setPassword("admin".toCharArray());
		options.setCleanSession(false);

		MqttClient client = new MqttClient("tcp://localhost:1883", "test.mqtt.client", new MemoryPersistence());
		client.connect(options);

		while (true) {
			client.publish("test/topic", "Test Message".getBytes(), 2, false);
			Thread.sleep(1000);
		}
	}

	@Test
	// @Ignore
	public void testVirtualTopicWithJMS() throws Exception {
		String consumerId = "test.jms.consumer.jms";
		MessageConsumer consumer = session.createDurableSubscriber(session.createTopic("test.topic"), consumerId);
		consumer.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(Message message) {
				System.out.println(message);
			}

		});

		MessageProducer producer = session.createProducer(session.createTopic("VirtualTopic.test.topic"));

		while (true) {
			producer.send(session.createTextMessage("Test Message"));
			Thread.sleep(1000);
		}
	}

}
