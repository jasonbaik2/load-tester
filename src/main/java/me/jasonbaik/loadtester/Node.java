package me.jasonbaik.loadtester;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import me.jasonbaik.loadtester.constant.StringConstants;

import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.util.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class Node implements MessageListener {

	private static final Logger logger = LogManager.getLogger(Node.class);

	enum Command {
		ACQUIRE, ACQUIREACK, SETUPSENDER, SETUPRECEIVER, SETUPACK, SETUPRECEIVERACK, ATTACK, ATTACKACK, COLLECT, COLLECTACK, RELEASE, RELEASEACK, ERROR;
	}

	private String uuid = UUID.randomUUID().toString();

	private ConnectionFactory connFactory;
	private Connection conn;
	private Session session;
	private Queue queue;
	private MessageConsumer queueConsumer;

	private Topic clientTopic;

	@PostConstruct
	public void init() throws URISyntaxException, JMSException {
		conn = connFactory.createConnection();
		session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

		queue = this.getSession().createQueue("node-" + getUuid());
		queueConsumer = this.getSession().createConsumer(queue);
		queueConsumer.setMessageListener(this);

		clientTopic = session.createTopic(StringConstants.CLIENT);

		conn.start();
	}

	protected void destroy() throws Exception {
		queueConsumer.close();
		session.close();
		conn.close();
	}

	void sendCommand(Destination dest, Command command, byte[] payload) throws JMSException {
		Message msg = createCommandMessage(command, payload);
		msg.setJMSReplyTo(queue);

		MessageProducer producer = session.createProducer(dest);
		producer.send(msg);
		producer.close();
	}

	private Message createCommandMessage(Command command, byte[] payload) throws JMSException {
		BytesMessage msg = new ActiveMQBytesMessage();
		msg.setStringProperty(StringConstants.UUID, uuid.toString());
		msg.setStringProperty(StringConstants.COMMAND, command.name());

		if (null != payload) {
			msg.writeBytes(payload);
		}

		return msg;
	}

	static byte[] readPayload(Message msg) throws JMSException {
		if (msg instanceof BytesMessage) {
			BytesMessage bytesMsg = (BytesMessage) msg;
			byte[] bytes = new byte[(int) bytesMsg.getBodyLength()];
			bytesMsg.readBytes(bytes);
			return bytes;
		}

		throw new IllegalArgumentException();
	}

	static <T> byte[] writeObject(T obj) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(obj);
		oos.close();

		return os.toByteArray();
	}

	static <T> T readObject(Message msg, Class<T> clazz) throws IOException, ClassNotFoundException, JMSException {
		byte[] bytes = readPayload(msg);

		if (bytes != null && bytes.length > 0) {
			ByteArrayInputStream is = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(is);
			return clazz.cast(ois.readObject());
		}

		return null;
	}

	static String readUserInput(String prompt) throws IOException {
		logger.info(prompt);
		return new BufferedReader(new InputStreamReader(System.in)).readLine();
	}

	public ConnectionFactory getConnFactory() {
		return connFactory;
	}

	public void setConnFactory(ConnectionFactory connFactory) {
		this.connFactory = connFactory;
	}

	public Connection getConn() {
		return conn;
	}

	public void setConn(Connection conn) {
		this.conn = conn;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Topic getClientTopic() {
		return clientTopic;
	}

	public void setClientTopic(Topic clientTopic) {
		this.clientTopic = clientTopic;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Queue getQueue() {
		return queue;
	}

	public void setQueue(Queue queue) {
		this.queue = queue;
	}

}