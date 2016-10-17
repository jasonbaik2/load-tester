package me.jasonbaik.loadtester.receiver.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

import me.jasonbaik.loadtester.constant.StringConstants;
import me.jasonbaik.loadtester.receiver.AbstractReceiver;
import me.jasonbaik.loadtester.valueobject.Broker;
import me.jasonbaik.loadtester.valueobject.Payload;
import me.jasonbaik.loadtester.valueobject.Protocol;
import me.jasonbaik.loadtester.valueobject.ReportData;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ActiveMQQueueDrainer extends AbstractReceiver<ActiveMQQueueDrainerConfig> implements MessageListener {

	private static final Logger logger = LogManager.getLogger(ActiveMQQueueDrainer.class);

	private String uuid = UUID.randomUUID().toString();
	private ActiveMQConnectionFactory connFactory;
	private List<Connection> conns;
	private Map<String, Long> inTimes = Collections.synchronizedMap(new HashMap<String, Long>());

	private AtomicInteger dequeueCount = new AtomicInteger();

	public ActiveMQQueueDrainer(ActiveMQQueueDrainerConfig config) {
		super(config);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init() throws Exception {
		logger.info("Initializing JMS connections");

		Broker broker = getConfig().getBrokers().get(0);

		connFactory = new ActiveMQConnectionFactory(broker.getUsername(), broker.getPassword(), "tcp://" + broker.getHostname() + ":" + broker.getConnectors().get(Protocol.JMS).getPort());
		connFactory.setAlwaysSessionAsync(false);

		conns = Collections.synchronizedList(new ArrayList<Connection>(getConfig().getNumJMSConnections()));

		for (int i = 0; i < getConfig().getNumJMSConnections(); i++) {
			Connection conn = connFactory.createConnection();
			conn.setClientID(uuid + "-" + i);
			Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
			session.createConsumer(session.createQueue(getConfig().getQueue())).setMessageListener(this);
			conns.add(conn);
		}

		logger.info("Successfully established " + getConfig().getNumJMSConnections() + " JMS connections");
	}

	@Override
	public void destroy() throws JMSException {
		logger.info("Closing JMS connections");

		synchronized (conns) {
			for (Connection c : conns) {
				c.close();
			}

			conns.clear();
		}
	}

	@Override
	public void receive() throws JMSException {
		setState("Dequeuing");

		synchronized (conns) {
			for (Connection c : conns) {
				c.start();
			}
		}
	}

	@Override
	public void onMessage(Message message) {
		if (message instanceof BytesMessage) {
			BytesMessage bytesMessage = (BytesMessage) message;

			try {
				byte[] payload = new byte[(int) bytesMessage.getBodyLength()];
				bytesMessage.readBytes(payload);
				inTimes.put(Payload.extractUniqueId(payload), message.getLongProperty(StringConstants.JMSACTIVEMQBROKERINTIME));
				dequeueCount.incrementAndGet();

			} catch (JMSException e) {
				logger.error("Failed to record queue out time", e);
			}
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

		return new ArrayList<ReportData>(Arrays.asList(new ReportData[] { new ReportData("JMS_In_Times.csv", sb.toString().getBytes()) }));
	}

	@Override
	public void log() {
		System.out.print(getState());
		System.out.print("\tDequeued: ");
		System.out.print(dequeueCount);
		System.out.print("\n");
	}

	public Map<String, Long> getInTimes() {
		return inTimes;
	}

	public void setInTimes(Map<String, Long> inTimes) {
		this.inTimes = inTimes;
	}

}