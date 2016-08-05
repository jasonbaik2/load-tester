package me.jasonbaik.loadtester.receiver.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import me.jasonbaik.loadtester.receiver.Receiver;
import me.jasonbaik.loadtester.reporter.Reportable;
import me.jasonbaik.loadtester.valueobject.Broker;
import me.jasonbaik.loadtester.valueobject.ReportData;

import org.apache.log4j.Logger;

public class BrokerThreadCountCollector extends Receiver<BrokerThreadCountCollectorConfig> implements Reportable<ReportData> {

	private static final Logger logger = Logger.getLogger(BrokerThreadCountCollector.class);

	private volatile List<BrokerJMX> connectors;
	private volatile ScheduledExecutorService es;

	public BrokerThreadCountCollector(BrokerThreadCountCollectorConfig config) {
		super(config);
		// TODO Auto-generated constructor stub
	}

	private static class BrokerJMX {
		public BrokerJMX(String id, JMXConnector connector, MBeanServerConnection conn) {
			super();
			this.id = id;
			this.connector = connector;
			this.conn = conn;
		}

		String id;
		JMXConnector connector;
		MBeanServerConnection conn;
		StringBuilder sb = new StringBuilder("Broker ID,time,threadCount");
	}

	@Override
	public void init() throws Exception {
		connectors = Collections.synchronizedList(new ArrayList<BrokerJMX>(getConfig().getBrokers().size()));

		for (Broker b : getConfig().getBrokers()) {
			HashMap<String, Object> env = new HashMap<String, Object>();
			env.put(JMXConnector.CREDENTIALS, new String[] { b.getUsername(), b.getPassword() });
			JMXServiceURL target = new JMXServiceURL(b.getJmxUrl());

			JMXConnector connector = JMXConnectorFactory.connect(target, env);
			MBeanServerConnection remote = connector.getMBeanServerConnection();
			connectors.add(new BrokerJMX(b.getHostname(), connector, remote));
		}
	}

	@Override
	public void destroy() throws Exception {
		es.shutdownNow();

		synchronized (connectors) {
			for (BrokerJMX b : connectors) {
				b.connector.close();
			}

			connectors.clear();
		}
	}

	@Override
	public synchronized void receive() throws Exception {
		es = Executors.newSingleThreadScheduledExecutor();
		es.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				synchronized (connectors) {
					for (BrokerJMX b : connectors) {
						try {
							b.sb.append(b.id).append(",").append(System.currentTimeMillis()).append(",").append(b.conn.getAttribute(new ObjectName("java.lang:type=Threading"), "ThreadCount"))
									.append("\n");
						} catch (AttributeNotFoundException e) {
							logger.error(e, e);
						} catch (InstanceNotFoundException e) {
							logger.error(e, e);
						} catch (MalformedObjectNameException e) {
							logger.error(e, e);
						} catch (MBeanException e) {
							logger.error(e, e);
						} catch (ReflectionException e) {
							logger.error(e, e);
						} catch (IOException e) {
							logger.error(e, e);
						}
					}
				}
			}

		}, 0, getConfig().getCollectInterval(), getConfig().getCollectIntervalUnit());
	}

	@Override
	public void log() {
		System.out.println("Collecting broker thread count stats...");
	}

	@Override
	public synchronized ArrayList<ReportData> report() throws InterruptedException {
		ArrayList<ReportData> data = new ArrayList<ReportData>(getConfig().getBrokers().size());

		synchronized (connectors) {
			for (BrokerJMX b : connectors) {
				data.add(new ReportData("Broker_Thread_Count_Stats" + b.id + ".csv", b.sb.toString().getBytes()));
			}
		}

		return data;
	}

}