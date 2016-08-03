package me.jasonbaik.loadtester.receiver.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import me.jasonbaik.loadtester.constant.StringConstants;
import me.jasonbaik.loadtester.receiver.Receiver;
import me.jasonbaik.loadtester.valueobject.Broker;
import me.jasonbaik.loadtester.valueobject.Payload;
import me.jasonbaik.loadtester.valueobject.Protocol;
import me.jasonbaik.loadtester.valueobject.ReportData;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JMSReplyingJMSConsumer extends Receiver<JMSReplyingJMSConsumerConfig> implements MessageListener {

	private static final Logger logger = LogManager.getLogger(JMSReplyingJMSConsumer.class);

	private ConnectionFactory connFactory;
	private Connection consumerConn;
	private Connection producerConn;
	private Session consumerSession;
	private Session producerSession;
	private MessageConsumer consumer;
	private Map<String, Long> inTimes = Collections.synchronizedMap(new HashMap<String, Long>());

	private volatile int publishedCount;

	public JMSReplyingJMSConsumer(JMSReplyingJMSConsumerConfig config) {
		super(config);
	}

	@Override
	public void init() throws Exception {
		Broker broker = getConfig().getBrokers().get(0);

		connFactory = new ActiveMQConnectionFactory(broker.getUsername(), broker.getPassword(), "tcp://" + broker.getHostname() + ":" + broker.getConnectors().get(Protocol.JMS).getPort()
				+ "?jms.useAsyncSend=true");
		consumerConn = connFactory.createConnection();
		consumerSession = consumerConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		consumer = consumerSession.createConsumer(consumerSession.createQueue(getConfig().getQueue()));

		producerConn = connFactory.createConnection();
		producerSession = producerConn.createSession(false, Session.AUTO_ACKNOWLEDGE);

		logger.info("Successfully established a JMS connection");
	}

	@Override
	public void destroy() throws JMSException {
		logger.info("Closing JMS connection");
		consumerConn.close();
		producerConn.close();
	}

	@Override
	public void receive() throws JMSException {
		consumer.setMessageListener(this);
		consumerConn.start();
	}

	@Override
	public synchronized void onMessage(Message message) {

		if (message instanceof BytesMessage) {
			BytesMessage bytesMessage = (BytesMessage) message;

			try {
				byte[] payload = new byte[(int) bytesMessage.getBodyLength()];
				bytesMessage.readBytes(payload);

				String[] idPair = Payload.extractIdPair(payload);
				String mqttReplyTopic = idPair[0];

				logger.debug("Publishing a reply to the MQTT client uuid=" + mqttReplyTopic);
				MessageProducer producer = producerSession.createProducer(consumerSession.createTopic(mqttReplyTopic));
				producer.send(bytesMessage);
				publishedCount++;
				producer.close();

				inTimes.put(Payload.extractUniqueId(payload), message.getLongProperty(StringConstants.JMSACTIVEMQBROKERINTIME));

			} catch (JMSException e) {
				logger.error("Failed to publish a reply", e);
			}
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public ArrayList<ReportData> report() throws InterruptedException {
		StringBuilder sb = new StringBuilder("MessageId,").append(StringConstants.JMSACTIVEMQBROKERINTIME).append("\n");

		synchronized (inTimes) {
			for (Iterator<Entry<String, Long>> iter = inTimes.entrySet().iterator(); iter.hasNext();) {
				Entry<String, Long> entry = iter.next();
				sb.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
			}
		}

		return new ArrayList<ReportData>(Arrays.asList(new ReportData[] { new ReportData("MQTTReplyingJMSConsumer_JMS_In_Times.csv", sb.toString().getBytes()) }));
	}

	@Override
	public void log() {
		System.out.print("Published: ");
		System.out.print(publishedCount);
		System.out.print("\n");
	}

}