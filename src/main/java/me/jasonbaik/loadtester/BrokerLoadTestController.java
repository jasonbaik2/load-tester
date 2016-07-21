package me.jasonbaik.loadtester;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import me.jasonbaik.loadtester.constant.StringConstants;
import me.jasonbaik.loadtester.valueobject.Broker;
import me.jasonbaik.loadtester.valueobject.Receive;
import me.jasonbaik.loadtester.valueobject.ReportData;
import me.jasonbaik.loadtester.valueobject.Scenario;
import me.jasonbaik.loadtester.valueobject.Send;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BrokerLoadTestController<S1, S2, R1> extends Node {

	private static final Logger logger = LogManager.getLogger(BrokerLoadTestController.class);

	private Map<String, Queue> clientUuidQueueMap = new HashMap<String, Queue>();
	private Map<String, List<ReportData>> reportData = new HashMap<String, List<ReportData>>();
	private BlockingQueue<Message> clientMessages = new LinkedBlockingQueue<Message>();

	private List<Broker> brokers;
	private boolean gcBroker = true;

	private List<Scenario<S1, S2, R1>> scenarios;
	private Scenario<S1, S2, R1> currentScenario;

	private void start() throws Exception {
		try {
			for (Scenario<S1, S2, R1> scenario : scenarios) {
				this.currentScenario = scenario;

				while (true) {
					try {
						if (gcBroker) {
							gcBrokers();
						}

						discoverClients();
						setupClients();
						attack();
						collectData();
						report();

					} catch (Exception e) {
						logger.error("Failed to run the scenario=" + currentScenario, e);

					} finally {
						releaseClients();

						if (!promptForRerun()) {
							break;
						}
					}
				}
			}
		} finally {
			destroy();
		}
	}

	private boolean promptForRerun() {
		boolean repeat = false;

		while (true) {
			String answer = readUserInput("Rerun the test? (y/n): ");

			if ("y".equalsIgnoreCase(answer)) {
				repeat = true;
				break;
			} else if ("n".equalsIgnoreCase(answer)) {
				break;
			}
		}
		return repeat;
	}

	private void gcBrokers() throws IOException, InstanceNotFoundException, MalformedObjectNameException, MBeanException, ReflectionException, InterruptedException {
		logger.info("GC'ing the brokers via JMX before the attack");

		for (Broker b : brokers) {
			HashMap<String, Object> env = new HashMap<String, Object>();
			env.put(JMXConnector.CREDENTIALS, new String[] { b.getUsername(), b.getPassword() });
			JMXServiceURL target = new JMXServiceURL(b.getJmxUrl());

			JMXConnector connector = JMXConnectorFactory.connect(target, env);
			MBeanServerConnection remote = connector.getMBeanServerConnection();
			remote.invoke(new ObjectName("java.lang:type=Memory"), "gc", null, null);
			connector.close();
		}

		logger.info("Sleeping 5 seconds to allow the broker to GC");
		Thread.sleep(5000);
	}

	private void discoverClients() throws JMSException, InterruptedException {
		logger.info("The scenario requires " + currentScenario.getSends().size() + " senders and " + currentScenario.getReceives().size() + " receivers");

		int numClients = currentScenario.getSends().size() + currentScenario.getReceives().size();

		while (clientUuidQueueMap.size() < numClients) {
			logger.info("Discovering clients...");
			sendCommand(this.getClientTopic(), Command.ACQUIRE, null);

			Message msg;

			while (null != (msg = clientMessages.poll(1000, TimeUnit.MILLISECONDS))) {
				if (Command.ACQUIREACK.name().equals(msg.getStringProperty(StringConstants.COMMAND))) {
					if (clientUuidQueueMap.size() < numClients) {
						String replierUUID = msg.getStringProperty(StringConstants.UUID);

						if (!clientUuidQueueMap.keySet().contains(msg.getStringProperty(StringConstants.UUID))) {
							logger.info("Client " + replierUUID + " replied.");
							clientUuidQueueMap.put(replierUUID, (Queue) msg.getJMSReplyTo());
						}

					} else {
						// More clients acquired than needed. Release it
						sendCommand(msg.getJMSReplyTo(), Command.RELEASE, null);
						break;
					}
				}
			}

			continue;
		}

		logger.info("Successfully discovered " + numClients + " clients");
		clientMessages.clear();
	}

	private void setupClients() throws Exception {
		logger.info("Setting up sender clients...");

		Iterator<Entry<String, Queue>> clientQueueIter = clientUuidQueueMap.entrySet().iterator();

		for (Send<S1, S2> send : currentScenario.getSends()) {
			Entry<String, Queue> entry = clientQueueIter.next();
			send.setClientUUID(entry.getKey());
			this.sendCommand(entry.getValue(), Command.SETUPSENDER, writeObject(send));
		}

		int readySenders = 0;

		while (readySenders < currentScenario.getSends().size()) {
			Message msg = clientMessages.take();
			String clientUUID = msg.getStringProperty(StringConstants.UUID);
			String replyCommand = msg.getStringProperty(StringConstants.COMMAND);

			if (Command.SETUPACK.name().equals(replyCommand)) {
				logger.info("Client uuid=" + clientUUID + " set up successfully");
				readySenders++;

			} else if (Command.ERROR.name().equals(replyCommand)) {
				throw new Exception("Client uuid=" + clientUUID + " failed during setup. Aborting the test...");
			}
		}

		logger.info("Setting up receiver clients...");

		for (Receive<R1> receive : currentScenario.getReceives()) {
			Entry<String, Queue> entry = clientQueueIter.next();
			receive.setClientUUID(entry.getKey());
			this.sendCommand(entry.getValue(), Command.SETUPRECEIVER, writeObject(receive));
		}

		int readyReceivers = 0;

		while (readyReceivers < currentScenario.getReceives().size()) {
			Message msg = clientMessages.take();
			String clientUUID = msg.getStringProperty(StringConstants.UUID);
			String replyCommand = msg.getStringProperty(StringConstants.COMMAND);

			if (Command.SETUPACK.name().equals(replyCommand)) {
				logger.info("Client uuid=" + clientUUID + " set up successfully");
				readyReceivers++;

			} else if (Command.ERROR.name().equals(replyCommand)) {
				throw new Exception("Client uuid=" + clientUUID + " failed during setup. Aborting the test...");
			}
		}

		logger.info("Successfully set up all clients");
	}

	private void attack() throws Exception {
		logger.info("Initiating attack...");

		sendCommand(this.getClientTopic(), Command.ATTACK, null);

		int doneClients = 0;

		while (doneClients < clientUuidQueueMap.size()) {
			Message msg = clientMessages.take();
			String clientUUID = msg.getStringProperty(StringConstants.UUID);
			String command = msg.getStringProperty(StringConstants.COMMAND);

			if (Command.ATTACKACK.name().equals(command)) {
				logger.info("Client uuid=" + clientUUID + " is attacking now...");
				doneClients++;

			} else if (Command.ERROR.name().equals(command)) {
				throw new Exception("Client uuid=" + clientUUID + " failed during the attack. Aborting the test...");
			}
		}

		logger.info("Successfully initiated the attack");

		if (currentScenario.getMaxAttackTimeSeconds() > 0) {
			logger.info("Sleeping for " + currentScenario.getMaxAttackTimeSeconds() + "s before stopping the attack");

		} else {
			readUserInput("Press ENTER to stop the attack and collect data: ");
		}
	}

	private void collectData() throws Exception {
		logger.info("Collecting flight data from the clients...");

		this.sendCommand(getClientTopic(), Command.COLLECT, null);

		int doneClients = 0;

		while (doneClients < clientUuidQueueMap.size()) {
			Message msg = clientMessages.take();
			String clientUUID = msg.getStringProperty(StringConstants.UUID);
			String command = msg.getStringProperty(StringConstants.COMMAND);

			if (Command.COLLECTACK.name().equals(command)) {
				@SuppressWarnings("unchecked")
				List<ReportData> data = readObject(msg, ArrayList.class);

				if (data != null) {
					reportData.put(clientUUID, data);
				}

				logger.info("Successfully collected data from the client uuid=" + clientUUID);
				doneClients++;

			} else if (Command.ERROR.name().equals(command)) {
				throw new Exception("Client uuid=" + clientUUID + " failed during collecting data. Aborting the test...");
			}
		}

		logger.info("Successfully collected data from all clients");
	}

	private void report() throws IOException {
		logger.info("Generating a report based on the collected client data...");

		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String dir = currentScenario.getReportDir() + "/" + currentScenario.getName() + "_" + dateFormat.format(now);

		new File(dir).mkdirs();

		FileWriter fw = new FileWriter(dir + "/description.txt");

		fw.write("============================\n");
		fw.write("Sends\n");
		fw.write("============================\n");

		for (Send<S1, S2> s : currentScenario.getSends()) {
			fw.write("Client UUID=" + s.getClientUUID());
			fw.write("\n");
			fw.write("Sampler=" + s.getSamplerConfig().describe());
			fw.write("\n");
			fw.write("Sender=" + s.getSenderConfig().describe());
			fw.write("\n");
			fw.write("\n");
		}

		fw.write("============================\n");
		fw.write("Receives\n");
		fw.write("============================\n");

		for (Receive<R1> r : currentScenario.getReceives()) {
			fw.write("Client UUID=" + r.getClientUUID());
			fw.write("\n");
			fw.write("Receiver=" + r.getReceiverConfig().describe());
			fw.write("\n");
			fw.write("\n");
		}

		fw.close();

		for (Send<S1, S2> s : currentScenario.getSends()) {
			String reportDir = dir + "/" + s.getName();
			new File(reportDir).mkdirs();
			writeReportData(reportData.get(s.getClientUUID()), reportDir);
		}

		for (Receive<R1> r : currentScenario.getReceives()) {
			String reportDir = dir + "/" + r.getName();
			new File(reportDir).mkdirs();
			writeReportData(reportData.get(r.getClientUUID()), reportDir);
		}

		logger.info("Successfully generated all reports");
	}

	private void writeReportData(List<ReportData> reports, String clientDir) throws IOException {
		for (ReportData d : reports) {
			FileOutputStream os = null;

			try {
				os = new FileOutputStream(clientDir + "/" + d.getName());
				os.write(d.getData());

			} catch (IOException e) {
				logger.error(e);

			} finally {
				if (os != null) {
					os.close();
				}
			}
		}
	}

	@Override
	protected void destroy() throws Exception {
		releaseClients();
		super.destroy();
		logger.info("Successfully destroyed the controller");
	}

	private void releaseClients() throws JMSException {
		logger.info("Releasing the acquired clients...");

		sendCommand(getClientTopic(), Command.RELEASE, null);
		clientUuidQueueMap.clear();
		reportData.clear();
	}

	@Override
	public void onMessage(Message message) {
		clientMessages.add(message);
	}

	public List<Scenario<S1, S2, R1>> getScenarios() {
		return scenarios;
	}

	public void setScenarios(List<Scenario<S1, S2, R1>> scenarios) {
		this.scenarios = scenarios;
	}

	public boolean isGcBroker() {
		return gcBroker;
	}

	public void setGcBroker(boolean gcBroker) {
		this.gcBroker = gcBroker;
	}

	public List<Broker> getBrokers() {
		return brokers;
	}

	public void setBrokers(List<Broker> brokers) {
		this.brokers = brokers;
	}

	public static void main(String[] args) throws BeansException, Exception {
		if (args.length == 2) {
			System.setProperty("env", args[1]);

		} else {
			System.setProperty("env", "local");
		}

		@SuppressWarnings("resource")
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(args[0]);
		context.getBean(BrokerLoadTestController.class).start();
	}

}