package me.jasonbaik.loadtester;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import me.jasonbaik.loadtester.constant.StringConstants;
import me.jasonbaik.loadtester.receiver.Receiver;
import me.jasonbaik.loadtester.receiver.ReceiverFactory;
import me.jasonbaik.loadtester.reporter.Loggable;
import me.jasonbaik.loadtester.sender.Sender;
import me.jasonbaik.loadtester.sender.SenderFactory;
import me.jasonbaik.loadtester.valueobject.Receive;
import me.jasonbaik.loadtester.valueobject.ReportData;
import me.jasonbaik.loadtester.valueobject.Send;

public class Client<S1, R1> extends Node {

	private static final Logger logger = LogManager.getLogger(Client.class);

	private AtomicReference<Queue> owningControllerQueue = new AtomicReference<Queue>(null);

	private MessageConsumer clientTopicConsumer;

	private volatile Sender<S1> sender;
	private volatile Receiver receiver;

	private volatile Thread attackThread;

	// Check status of attack periodically
	private volatile ScheduledExecutorService statusService;

	private String clientLog;

	private void start() throws URISyntaxException, JMSException {
		clientTopicConsumer = this.getSession().createConsumer(getClientTopic());
		clientTopicConsumer.setMessageListener(new ClientTopicListener());

		logger.info("Successfully initialized the client with uuid=" + getUuid().toString());
	}

	private void setupSender(Send<S1> send) throws Exception {
		logger.info("Setting up a sender according to the test config: " + send.toString());

		this.sender = SenderFactory.newInstance(send.getSenderConfig());

		logger.info("Successfully set up the sender");
	}

	private void setupReceiver(Receive<R1> receive) throws Exception {
		logger.info("Setting up a receiver according to the test config: " + receive.toString());

		this.receiver = ReceiverFactory.newInstance(receive.getReceiverConfig());

		logger.info("Successfully set up the receiver");
	}

	private void attack() throws Exception {
		logger.info("GC'ing prior to the attack");
		System.gc();

		attackThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (sender != null) {
						logger.info("Sending...");
						startStatusService(sender);
						sender.send();
					} else if (receiver != null) {
						logger.info("Receiving...");
						startStatusService(receiver);
						receiver.receive();
					}
				} catch (InterruptedException e) {
					logger.warn("Attack interrupted", e);

				} catch (Exception e) {
					logger.error("Error", e);

					try {
						sendCommand(owningControllerQueue.get(), Command.ERROR, e.getMessage() != null ? e.getMessage().getBytes() : null);
					} catch (JMSException e1) {
						logger.error(e1);
					}
				}
			}

		}, (sender != null ? "Send" : "Receive") + " Thread");

		attackThread.start();
	}

	private void startStatusService(final Loggable loggable) {
		statusService = Executors.newSingleThreadScheduledExecutor();
		statusService.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				loggable.log();
			}

		}, 0, 2, TimeUnit.SECONDS);
	}

	private List<ReportData> collect() throws InterruptedException, JMSException, IOException {
		logger.info("Sending the collected flight data to the controller...");

		interrupt();

		List<ReportData> data = null;

		if (sender != null && this.sender.report() != null) {
			data = sender.report();

		} else if (receiver != null && this.receiver.report() != null) {
			data = receiver.report();
		}

		if (this.getClientLog() != null && this.getClientLog().length() > 0) {
			try {
				File file = new File(this.getClientLog());

				if (file.exists()) {
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					byte[] buffer = new byte[1024];
					int numRead = 0;

					InputStream is = new FileInputStream(file);

					while ((numRead = is.read(buffer)) > 0) {
						os.write(buffer, 0, numRead);
					}

					is.close();
					os.close();
					data.add(new ReportData("client.log", os.toByteArray()));
				}
			} catch (Exception e) {
				logger.error("Failed to collect the client log", e);
			}
		}

		return data;
	}

	private void interrupt() {
		logger.info("Interrupting the client");

		if (attackThread != null) {
			attackThread.interrupt();
		}

		if (statusService != null) {
			statusService.shutdownNow();
		}
	}

	@Override
	protected void destroy() {
		logger.info("Destroying the client...");

		interrupt();

		if (sender != null) {
			try {
				sender.destroy();
			} catch (Exception e) {
				logger.error("Failed to destroy the sender", e);
			}
		}

		if (receiver != null) {
			try {
				receiver.destroy();
			} catch (Exception e) {
				logger.error("Failed to destroy the receiver", e);
			}
		}

		logger.info("Successfully destroyed the client");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMessage(Message message) {
		String command = null;

		try {
			command = message.getStringProperty(StringConstants.COMMAND);

			if (Command.RELEASE.name().equals(command)) {
				this.destroy();

				logger.info("Client released");
				sendCommand(owningControllerQueue.get(), Command.RELEASEACK, null);
				owningControllerQueue.set(null);
				return;
			}

			if (Command.SETUPSENDER.name().equals(command)) {
				this.setupSender(readObject(message, Send.class));
				sendCommand(owningControllerQueue.get(), Command.SETUPACK, null);

			} else if (Command.SETUPRECEIVER.name().equals(command)) {
				this.setupReceiver(readObject(message, Receive.class));
				sendCommand(owningControllerQueue.get(), Command.SETUPACK, null);

			} else if (Command.ATTACK.name().equals(command)) {
				this.attack();
				sendCommand(owningControllerQueue.get(), Command.ATTACKACK, null);

			} else if (Command.COLLECT.name().equals(command)) {
				sendCommand(owningControllerQueue.get(), Command.COLLECTACK, writeObject(this.collect()));
			}

		} catch (JMSException e) {
			logger.error("JMSException", e);
			owningControllerQueue.set(null);

		} catch (Exception e) {
			logger.error("Failed to run the command", e);

			try {
				sendCommand(owningControllerQueue.get(), Command.ERROR, null);
			} catch (JMSException e1) {
				logger.error(e);
			}
		}
	}

	private class ClientTopicListener implements MessageListener {

		@Override
		public void onMessage(Message message) {
			try {
				String command = message.getStringProperty(StringConstants.COMMAND);

				if (Command.ACQUIRE.name().equals(command)) {
					String sender = message.getStringProperty(StringConstants.UUID);
					Destination senderReplyTo = message.getJMSReplyTo();

					logger.debug("Received an ACQUIRE request from the controller uuid=" + sender);

					if (owningControllerQueue.compareAndSet(null, (Queue) senderReplyTo)) {
						logger.info("The client is now in use by the controller uuid=" + sender);
						Client.this.sendCommand(senderReplyTo, Command.ACQUIREACK, null);

					} else {
						logger.debug("The client is in use. Ignoring the ACQUIRE request from the controller uuid=" + sender);
						return;
					}
				} else if (message.getJMSReplyTo() instanceof Queue) {
					Queue queue = (Queue) message.getJMSReplyTo();

					if (owningControllerQueue.get() != null && owningControllerQueue.get().getQueueName().equals(queue.getQueueName())) {
						Client.this.onMessage(message);
					}
				}
			} catch (JMSException e) {
				logger.error("Failed to process a client topic message", e);
			}
		}

	}

	public String getClientLog() {
		return clientLog;
	}

	public void setClientLog(String clientLog) {
		this.clientLog = clientLog;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException, URISyntaxException, JMSException, InterruptedException, JAXBException {
		if (args.length == 1) {
			System.setProperty("env", args[0]);

		} else {
			System.setProperty("env", "local");
		}

		@SuppressWarnings("resource")
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("file:spring/context-client.xml");
		context.getBean(Client.class).start();
	}

}