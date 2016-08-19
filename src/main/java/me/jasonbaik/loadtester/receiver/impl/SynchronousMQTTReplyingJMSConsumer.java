package me.jasonbaik.loadtester.receiver.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.jms.MessageListener;

import me.jasonbaik.loadtester.client.MQTTClientFactory;
import me.jasonbaik.loadtester.reporter.impl.MQTTFlightTracer;
import me.jasonbaik.loadtester.util.SSLUtil;
import me.jasonbaik.loadtester.valueobject.Broker;
import me.jasonbaik.loadtester.valueobject.MQTTFlightData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.mqtt.client.Future;
import org.fusesource.mqtt.client.FutureConnection;
import org.fusesource.mqtt.client.MQTT;

public class SynchronousMQTTReplyingJMSConsumer extends AbstractMQTTReplyingJMSConsumer<SynchronousMQTTReplyingJMSConsumerConfig> implements MessageListener {

	private static final Logger logger = LogManager.getLogger(SynchronousMQTTReplyingJMSConsumer.class);

	private String mqttUuid = UUID.randomUUID().toString();
	private BlockingQueue<FutureConnection> mqttConns;
	private List<MQTTFlightTracer> tracers;

	public SynchronousMQTTReplyingJMSConsumer(SynchronousMQTTReplyingJMSConsumerConfig config) {
		super(config);
	}

	@Override
	protected void initMQTTConnections() throws Exception {
		mqttConns = new ArrayBlockingQueue<FutureConnection>(getConfig().getNumMQTTConnections());
		tracers = new ArrayList<MQTTFlightTracer>(getConfig().getNumMQTTConnections());

		List<Future<Void>> connFutures = new ArrayList<Future<Void>>(getConfig().getNumMQTTConnections());

		for (int i = 0; i < getConfig().getNumMQTTConnections(); i++) {
			Broker broker = getConfig().getBrokers().get(i % getConfig().getBrokers().size());
			MQTT client = new MQTT();
			client.setHost(MQTTClientFactory.getFusesourceConnectionUrl(broker, getConfig().isSsl()));
			client.setClientId(mqttUuid + "-" + i);
			client.setCleanSession(getConfig().isCleanSession());
			client.setUserName(broker.getUsername());
			client.setPassword(broker.getPassword());
			client.setKeepAlive((short) 0);

			if (getConfig().isSsl()) {
				client.setSslContext(SSLUtil.createSSLContext(getConfig().getKeyStore(), getConfig().getKeyStorePassword(), getConfig().getTrustStore(), getConfig().getTrustStorePassword()));
			}

			MQTTFlightTracer tracer = new MQTTFlightTracer();
			client.setTracer(tracer);
			tracers.add(tracer);

			FutureConnection mqttConn = client.futureConnection();
			Future<Void> future = mqttConn.connect();
			connFutures.add(future);

			mqttConns.put(mqttConn);
		}

		for (int i = 0; i < getConfig().getNumMQTTConnections(); i++) {
			logger.info("Waiting on MQTT Connection #" + i);
			connFutures.get(i).await();
		}
	}

	@Override
	protected void destroyMQTTConnections() {
		for (FutureConnection conn : mqttConns) {
			conn.kill();
		}

		mqttConns.clear();
	}

	@Override
	protected void reply(byte[] payload, String mqttReplyTopic) throws InterruptedException {
		FutureConnection mqttConn = mqttConns.take();
		Future<Void> future = mqttConn.publish(mqttReplyTopic, payload, getConfig().getQos(), false);
		getPublishedCount().incrementAndGet();

		try {
			future.await();
			getSuccessCount().incrementAndGet();

		} catch (Exception e) {
			logger.error("Failed to publish a reply", e);
			getFailureCount().incrementAndGet();
		}

		mqttConns.put(mqttConn); // Return the client to the pool
	}

	@Override
	protected Collection<MQTTFlightData> collectFlightData() {
		List<MQTTFlightData> flightData = new LinkedList<MQTTFlightData>();

		for (MQTTFlightTracer tracer : this.tracers) {
			flightData.addAll(tracer.getFlightData());
		}

		return flightData;
	}

}