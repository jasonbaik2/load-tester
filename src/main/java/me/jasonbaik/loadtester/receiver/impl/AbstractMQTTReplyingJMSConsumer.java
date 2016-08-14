package me.jasonbaik.loadtester.receiver.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

import me.jasonbaik.loadtester.constant.StringConstants;
import me.jasonbaik.loadtester.receiver.AbstractReceiver;
import me.jasonbaik.loadtester.reporter.impl.MQTTFlightTracer;
import me.jasonbaik.loadtester.valueobject.Broker;
import me.jasonbaik.loadtester.valueobject.MQTTFlightData;
import me.jasonbaik.loadtester.valueobject.Payload;
import me.jasonbaik.loadtester.valueobject.Protocol;
import me.jasonbaik.loadtester.valueobject.ReportData;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractMQTTReplyingJMSConsumer<T extends AbstractMQTTReplyingJMSConsumerConfig<?>> extends AbstractReceiver<T> implements MessageListener {

	private static final Logger logger = LogManager.getLogger(AbstractMQTTReplyingJMSConsumer.class);

	private String uuid = UUID.randomUUID().toString();
	private ActiveMQConnectionFactory connFactory;
	private List<Connection> conns;
	private Map<String, Long> inTimes = Collections.synchronizedMap(new HashMap<String, Long>());

	private AtomicInteger dequeueCount = new AtomicInteger();
	private AtomicInteger publishedCount = new AtomicInteger();
	private AtomicInteger successCount = new AtomicInteger();
	private AtomicInteger failureCount = new AtomicInteger();

	private volatile ExecutorService replyService;
	private BlockingQueue<BytesMessage> replyMessages;

	public AbstractMQTTReplyingJMSConsumer(T config) {
		super(config);
	}

	@Override
	public void init() throws Exception {
		replyService = Executors.newFixedThreadPool(getConfig().getNumReplyThreads());
		replyMessages = new LinkedBlockingQueue<BytesMessage>();

		logger.info("Initializing JMS connections");

		Broker broker = getConfig().getBrokers().get(0);

		connFactory = new ActiveMQConnectionFactory(broker.getUsername(), broker.getPassword(), "tcp://" + broker.getHostname() + ":" + broker.getConnectors().get(Protocol.JMS).getPort());
		connFactory.setAlwaysSessionAsync(false);

		conns = new ArrayList<Connection>(getConfig().getNumJMSConnections());

		for (int i = 0; i < getConfig().getNumJMSConnections(); i++) {
			Connection conn = connFactory.createConnection();
			conn.setClientID(uuid + "-" + i);
			Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
			session.createConsumer(session.createQueue(getConfig().getQueue())).setMessageListener(this);
			conns.add(conn);
		}

		logger.info("Successfully established " + getConfig().getNumJMSConnections() + " JMS connections");

		setState("Initializing MQTT Connections");
		initMQTTConnections();

		logger.info("Successfully established reply " + getConfig().getNumMQTTConnections() + " MQTT connections");
	}

	@Override
	public void destroy() throws JMSException {
		if (replyService != null) {
			replyService.shutdownNow();
		}

		if (replyMessages != null) {
			replyMessages.clear();
		}

		logger.info("Disconnecting MQTT connection");
		destroyMQTTConnections();

		logger.info("Closing JMS connections");

		for (Connection c : conns) {
			c.close();
		}

		conns.clear();

	}

	protected abstract void initMQTTConnections() throws Exception;

	protected abstract void destroyMQTTConnections();

	protected abstract void reply(byte[] payload, String mqttReplyTopic) throws InterruptedException;

	protected abstract Collection<MQTTFlightData> collectFlightData();

	@Override
	public void receive() throws JMSException {
		for (Connection c : conns) {
			c.start();
		}

		setState("Receiving/Replying");

		replyService.execute(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						BytesMessage message = replyMessages.take();
						byte[] payload = new byte[(int) message.getBodyLength()];
						message.readBytes(payload);

						String[] idPair = Payload.extractIdPair(payload);
						String mqttReplyTopic = idPair[0];

						logger.debug("Publishing a reply to the MQTT client uuid=" + mqttReplyTopic);
						reply(payload, mqttReplyTopic);

						inTimes.put(Payload.extractUniqueId(payload), message.getLongProperty(StringConstants.JMSACTIVEMQBROKERINTIME));

					} catch (JMSException e) {
						logger.error("Failed to reply", e);

					} catch (InterruptedException e) {
						logger.error("Interrupted", e);
					}
				}
			}

		});
	}

	@Override
	public void onMessage(Message message) {
		if (message instanceof BytesMessage) {
			replyMessages.add((BytesMessage) message);
			dequeueCount.incrementAndGet();
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

		ArrayList<ReportData> data = new ArrayList<ReportData>(Arrays.asList(new ReportData[] { new ReportData("JMS_In_Times.csv", sb.toString().getBytes()),
				new ReportData("MQTT_Flight_Data.csv", MQTTFlightTracer.toCsv(collectFlightData())) }));
		return data;
	}

	@Override
	public void log() {
		System.out.print(getState());
		System.out.print("\tDequeued: ");
		System.out.print(dequeueCount);
		System.out.print(", Published: ");
		System.out.print(publishedCount);
		System.out.print(", Success: ");
		System.out.print(successCount);
		System.out.print(", Failed: ");
		System.out.print(failureCount);
		System.out.print("\n");
	}

	public Map<String, Long> getInTimes() {
		return inTimes;
	}

	public void setInTimes(Map<String, Long> inTimes) {
		this.inTimes = inTimes;
	}

	public AtomicInteger getDequeueCount() {
		return dequeueCount;
	}

	public void setDequeueCount(AtomicInteger dequeueCount) {
		this.dequeueCount = dequeueCount;
	}

	public AtomicInteger getPublishedCount() {
		return publishedCount;
	}

	public void setPublishedCount(AtomicInteger publishedCount) {
		this.publishedCount = publishedCount;
	}

	public AtomicInteger getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(AtomicInteger successCount) {
		this.successCount = successCount;
	}

	public AtomicInteger getFailureCount() {
		return failureCount;
	}

	public void setFailureCount(AtomicInteger failureCount) {
		this.failureCount = failureCount;
	}

	public ExecutorService getReplyService() {
		return replyService;
	}

	public void setReplyService(ExecutorService replyService) {
		this.replyService = replyService;
	}

	public BlockingQueue<BytesMessage> getReplyMessages() {
		return replyMessages;
	}

	public void setReplyMessages(BlockingQueue<BytesMessage> replyMessages) {
		this.replyMessages = replyMessages;
	}

}