package me.jasonbaik.loadtester;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.apache.xbean.spring.context.ResourceXmlApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;

public class BrokerLoadTestController<S1, S2, R1> extends Node {

	private static final Logger logger = LogManager.getLogger(BrokerLoadTestController.class);

	private ApplicationContext controllerContext;

	private Map<String, Queue> clientQueueMap = new HashMap<String, Queue>();
	private Map<String, List<ReportData>> reportData = new HashMap<String, List<ReportData>>();
	private BlockingQueue<Message> clientMessages = new LinkedBlockingQueue<Message>();

	private List<Broker> brokers;

	private Scenario<S1, S2, R1> currentScenario;

	private void start() throws Exception {
		try {
			while (true) {
				List<Scenario<S1, S2, R1>> scenarios = loadScenarios();

				for (Scenario<S1, S2, R1> scenario : scenarios) {
					this.currentScenario = scenario;

					while (true) {
						try {
							discoverClients();
							setupClients();

							if (scenario.isGcBrokers()) {
								gcBrokers();
							}

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
			}
		} finally {
			destroy();
		}
	}

	private List<String> listTestContextFiles(File file, String parentPath, List<String> files) {
		String path = (parentPath == null ? file.getName() : parentPath + "/" + file.getName());

		if (file.isDirectory()) {
			File[] children = file.listFiles();
			Arrays.sort(children);

			for (File c : children) {
				listTestContextFiles(c, path, files);
			}
		} else {
			files.add(path);
		}

		return files;
	}

	private List<Scenario<S1, S2, R1>> loadScenarios() throws IOException {
		File testContextFile = null;

		while (true) {
			List<String> testContextFiles = listTestContextFiles(new File("scenarios"), null, new ArrayList<String>());

			for (int i = 0; i < testContextFiles.size(); i++) {
				System.out.println("[" + i + "]\t" + testContextFiles.get(i));
			}

			String answer = readUserInput("Select one of the test contexts, or enter a new path to test context: ");

			try {
				int num = Integer.parseInt(answer);

				if (0 <= num && num < testContextFiles.size()) {
					testContextFile = new File(testContextFiles.get(num));
					break;
				}
			} catch (NumberFormatException e) {
				testContextFile = new File(answer);

				if (!testContextFile.exists()) {
					logger.error("The file does not exist. Try again.");
				} else {
					break;
				}
			}
		}

		ResourceXmlApplicationContext testContext = new ResourceXmlApplicationContext(new FileSystemResource(testContextFile), controllerContext);

		try {
			return testContext.getBean("scenarios", List.class);
		} finally {
			testContext.close();
		}
	}

	private boolean promptForRerun() throws IOException {
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

		for (Broker b : currentScenario.getBrokers()) {
			HashMap<String, Object> env = new HashMap<String, Object>();
			env.put(JMXConnector.CREDENTIALS, new String[] { b.getUsername(), b.getPassword() });
			JMXServiceURL target = new JMXServiceURL(b.getJmxUrl());

			JMXConnector connector = JMXConnectorFactory.connect(target, env);
			MBeanServerConnection remote = connector.getMBeanServerConnection();
			remote.invoke(new ObjectName("java.lang:type=Memory"), "gc", null, null);
			connector.close();
		}

		logger.info("Sleeping 5 seconds to allow the brokers to GC");
		Thread.sleep(5000);
	}

	private void discoverClients() throws JMSException, InterruptedException {
		logger.info("The scenario requires " + currentScenario.getSends().size() + " senders and " + currentScenario.getReceives().size() + " receivers");

		int numClients = currentScenario.getSends().size() + currentScenario.getReceives().size();

		while (clientQueueMap.size() < numClients) {
			logger.info("Discovering clients...");
			sendCommand(this.getClientTopic(), Command.ACQUIRE, null);

			Message msg;

			while (null != (msg = clientMessages.poll(1000, TimeUnit.MILLISECONDS))) {
				if (Command.ACQUIREACK.name().equals(msg.getStringProperty(StringConstants.COMMAND))) {
					if (clientQueueMap.size() < numClients) {
						String replierUUID = msg.getStringProperty(StringConstants.UUID);

						if (!clientQueueMap.keySet().contains(msg.getStringProperty(StringConstants.UUID))) {
							logger.info("Client " + replierUUID + " replied.");
							clientQueueMap.put(replierUUID, (Queue) msg.getJMSReplyTo());
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

		Iterator<Entry<String, Queue>> clientQueueIter = clientQueueMap.entrySet().iterator();

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

		while (doneClients < clientQueueMap.size()) {
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

		while (doneClients < clientQueueMap.size()) {
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

	private void releaseClients() throws Exception {
		logger.info("Releasing the acquired clients...");

		sendCommand(getClientTopic(), Command.RELEASE, null);

		int doneClients = 0;

		while (doneClients < clientQueueMap.size()) {
			Message msg = clientMessages.take();
			String clientUUID = msg.getStringProperty(StringConstants.UUID);
			String command = msg.getStringProperty(StringConstants.COMMAND);

			if (Command.RELEASEACK.name().equals(command)) {
				logger.info("Successfully released the client uuid=" + clientUUID);
				doneClients++;

			} else if (Command.ERROR.name().equals(command)) {
				throw new Exception("Client uuid=" + clientUUID + " failed to be released. Aborting the test...");
			}
		}

		clientQueueMap.clear();
		reportData.clear();
	}

	@Override
	public void onMessage(Message message) {
		clientMessages.add(message);
	}

	public List<Broker> getBrokers() {
		return brokers;
	}

	public void setBrokers(List<Broker> brokers) {
		this.brokers = brokers;
	}

	public static void main(String[] args) throws BeansException, Exception {
		if (args.length == 1) {
			System.setProperty("env", args[0]);

		} else {
			System.setProperty("env", "local");
		}

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("file:spring/context-controller.xml");
		BrokerLoadTestController<?, ?, ?> controller = context.getBean(BrokerLoadTestController.class);
		controller.controllerContext = context;
		controller.start();
		context.close();
	}

}