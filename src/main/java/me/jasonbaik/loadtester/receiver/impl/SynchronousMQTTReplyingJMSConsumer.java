package me.jasonbaik.loadtester.receiver.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.jms.MessageListener;

import me.jasonbaik.loadtester.client.MQTTClientFactory;
import me.jasonbaik.loadtester.util.SSLUtil;
import me.jasonbaik.loadtester.valueobject.Broker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.mqtt.client.Future;
import org.fusesource.mqtt.client.FutureConnection;
import org.fusesource.mqtt.client.MQTT;

public class SynchronousMQTTReplyingJMSConsumer extends AbstractMQTTReplyingJMSConsumer<SynchronousMQTTReplyingJMSConsumerConfig> implements MessageListener {

	private static final Logger logger = LogManager.getLogger(SynchronousMQTTReplyingJMSConsumer.class);

	private BlockingQueue<FutureConnection> mqttConns;

	public SynchronousMQTTReplyingJMSConsumer(SynchronousMQTTReplyingJMSConsumerConfig config) {
		super(config);
	}

	@Override
	protected void initMQTTConnections() throws Exception {
		Broker broker = getConfig().getBrokers().get(0);

		mqttConns = new ArrayBlockingQueue<FutureConnection>(getConfig().getNumMQTTConnections());
		List<Future<Void>> connFutures = new ArrayList<Future<Void>>(getConfig().getNumMQTTConnections());

		for (int i = 0; i < getConfig().getNumMQTTConnections(); i++) {
			MQTT client = new MQTT();
			client.setHost(MQTTClientFactory.getFusesourceConnectionUrl(broker, getConfig().isSsl()));
			client.setClientId(getUuid() + "-" + i);
			client.setCleanSession(getConfig().isCleanSession());
			client.setUserName(broker.getUsername());
			client.setPassword(broker.getPassword());
			client.setKeepAlive((short) 0);
			client.setSslContext(SSLUtil.createSSLContext(getConfig().getKeyStore(), getConfig().getKeyStorePassword(), getConfig().getTrustStore(), getConfig().getTrustStorePassword()));
			client.setTracer(getTracer());

			FutureConnection mqttConn = client.futureConnection();
			Future<Void> future = mqttConn.connect();
			connFutures.add(future);

			mqttConns.put(mqttConn);
		}

		for (int i = 0; i < getConfig().getNumMQTTConnections(); i++) {
			setState("Waiting on MQTT Connection #" + i);
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

}