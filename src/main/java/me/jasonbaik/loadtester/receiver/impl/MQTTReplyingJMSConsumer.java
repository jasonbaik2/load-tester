package me.jasonbaik.loadtester.receiver.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import me.jasonbaik.loadtester.client.MQTTClientFactory;
import me.jasonbaik.loadtester.constant.StringConstants;
import me.jasonbaik.loadtester.receiver.Receiver;
import me.jasonbaik.loadtester.util.MQTTFlightTracer;
import me.jasonbaik.loadtester.util.SSLUtil;
import me.jasonbaik.loadtester.valueobject.Broker;
import me.jasonbaik.loadtester.valueobject.Payload;
import me.jasonbaik.loadtester.valueobject.Protocol;
import me.jasonbaik.loadtester.valueobject.ReportData;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.MQTT;

public class MQTTReplyingJMSConsumer extends Receiver<MQTTReplyingJMSConsumerConfig> implements MessageListener {

	private static final Logger logger = LogManager.getLogger(MQTTReplyingJMSConsumer.class);

	private String uuid = UUID.randomUUID().toString();
	private ConnectionFactory connFactory;
	private Connection conn;
	private Session session;
	private MessageConsumer consumer;
	private Map<String, Long> inTimes = Collections.synchronizedMap(new HashMap<String, Long>());

	private CallbackConnection mqttConn;
	private MQTTFlightTracer tracer = new MQTTFlightTracer();

	private AtomicInteger publishedCount = new AtomicInteger();
	private AtomicInteger successCount = new AtomicInteger();
	private AtomicInteger failureCount = new AtomicInteger();

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

	public MQTTReplyingJMSConsumer(MQTTReplyingJMSConsumerConfig config) {
		super(config);
	}

	@Override
	public void init() throws Exception {
		Broker broker = getConfig().getBrokers().get(0);

		connFactory = new ActiveMQConnectionFactory(broker.getUsername(), broker.getPassword(), "tcp://" + broker.getHostname() + ":" + broker.getConnectors().get(Protocol.JMS).getPort());
		conn = connFactory.createConnection();
		session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		consumer = session.createConsumer(session.createQueue(getConfig().getQueue()));

		logger.info("Successfully established a JMS connection");

		MQTT client = new MQTT();
		client.setHost(MQTTClientFactory.getFusesourceConnectionUrl(broker, getConfig().isSsl()));
		client.setClientId(uuid);
		client.setCleanSession(getConfig().isCleanSession());
		client.setUserName(broker.getUsername());
		client.setPassword(broker.getPassword());
		client.setKeepAlive((short) 0);
		client.setSslContext(SSLUtil.createSSLContext(getConfig().getKeyStore(), getConfig().getKeyStorePassword(), getConfig().getTrustStore(), getConfig().getTrustStorePassword()));
		client.setTracer(tracer);

		mqttConn = client.callbackConnection();

		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean success = new AtomicBoolean();

		mqttConn.connect(new Callback<Void>() {

			@Override
			public void onSuccess(Void value) {
				success.set(true);

				if (success.get()) {
					latch.countDown();
				}
			}

			@Override
			public void onFailure(Throwable value) {
				success.set(false);

				if (!success.get()) {
					latch.countDown();
				}
			}
		});

		latch.await();

		if (success.get()) {
			logger.info("Successfully established an MQTT connection");
		} else {
			throw new Exception("Failed to establish an MQTT connection");
		}
	}

	@Override
	public void destroy() throws JMSException {
		logger.info("Closing JMS connection");
		conn.close();

		logger.info("Disconnecting MQTT connection");
		mqttConn.kill(null);
	}

	@Override
	public void receive() throws JMSException {
		consumer.setMessageListener(this);
		conn.start();
	}

	@Override
	public void onMessage(Message message) {
		if (message instanceof BytesMessage) {
			BytesMessage bytesMessage = (BytesMessage) message;

			try {
				byte[] payload = new byte[(int) bytesMessage.getBodyLength()];
				bytesMessage.readBytes(payload);

				String[] idPair = Payload.extractIdPair(payload);
				String mqttReplyTopic = idPair[0];

				logger.debug("Publishing a reply to the MQTT client uuid=" + mqttReplyTopic);

				mqttConn.publish(mqttReplyTopic, payload, getConfig().getQos(), false, publishCallback);
				publishedCount.incrementAndGet();

				inTimes.put(Payload.extractUniqueId(payload), message.getLongProperty(StringConstants.JMSACTIVEMQBROKERINTIME));

			} catch (JMSException e) {
				throw new IllegalArgumentException();
			}
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public ArrayList<ReportData> report() throws InterruptedException {
		byte[] mqttFlightData = MQTTFlightTracer.toCsv(tracer.getFlightData());

		StringBuilder sb = new StringBuilder("MessageId,").append(StringConstants.JMSACTIVEMQBROKERINTIME).append("\n");

		synchronized (inTimes) {
			for (Iterator<Entry<String, Long>> iter = inTimes.entrySet().iterator(); iter.hasNext();) {
				Entry<String, Long> entry = iter.next();
				sb.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
			}
		}

		return new ArrayList<ReportData>(Arrays.asList(new ReportData[] { new ReportData("SynchronousMQTTReplyingJMSConsumer_MQTT_Flight_Data.csv", mqttFlightData),
				new ReportData("MQTTReplyingJMSConsumer_JMS_In_Times.csv", sb.toString().getBytes()) }));
	}

	@Override
	public void log() {
		System.out.print("Published: ");
		System.out.print(publishedCount);
		System.out.print(", Success: ");
		System.out.print(successCount);
		System.out.print(", Failed: ");
		System.out.print(failureCount);
		System.out.print("\n");
	}

}