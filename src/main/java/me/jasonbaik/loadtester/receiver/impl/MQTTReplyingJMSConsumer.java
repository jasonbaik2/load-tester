package me.jasonbaik.loadtester.receiver.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.MessageListener;

import me.jasonbaik.loadtester.client.MQTTClientFactory;
import me.jasonbaik.loadtester.util.SSLUtil;
import me.jasonbaik.loadtester.valueobject.Broker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.MQTT;

public class MQTTReplyingJMSConsumer extends AbstractMQTTReplyingJMSConsumer<MQTTReplyingJMSConsumerConfig> implements MessageListener {

	private static final Logger logger = LogManager.getLogger(MQTTReplyingJMSConsumer.class);

	private String mqttUuid = UUID.randomUUID().toString();
	private List<CallbackConnection> mqttConns;
	private AtomicInteger connIndex = new AtomicInteger();
	private CountDownLatch connectionLatch;

	private Callback<Void> connectCallback = new Callback<Void>() {

		@Override
		public void onSuccess(Void value) {
			connectionLatch.countDown();
		}

		@Override
		public void onFailure(Throwable value) {
			logger.error("Error", value);
		}

	};

	private Callback<Void> publishCallback = new Callback<Void>() {

		@Override
		public void onSuccess(Void value) {
			getSuccessCount().incrementAndGet();
		}

		@Override
		public void onFailure(Throwable value) {
			getFailureCount().incrementAndGet();
		}

	};

	public MQTTReplyingJMSConsumer(MQTTReplyingJMSConsumerConfig config) {
		super(config);
	}

	@Override
	protected void initMQTTConnections() throws Exception {
		Broker broker = getConfig().getBrokers().get(0);

		mqttConns = new ArrayList<CallbackConnection>(getConfig().getNumMQTTConnections());
		connectionLatch = new CountDownLatch(getConfig().getNumMQTTConnections());

		for (int i = 0; i < getConfig().getNumMQTTConnections(); i++) {
			MQTT client = new MQTT();
			client.setHost(MQTTClientFactory.getFusesourceConnectionUrl(broker, getConfig().isSsl()));
			client.setClientId(mqttUuid + "-" + i);
			client.setCleanSession(getConfig().isCleanSession());
			client.setUserName(broker.getUsername());
			client.setPassword(broker.getPassword());
			client.setKeepAlive((short) 0);
			client.setSslContext(SSLUtil.createSSLContext(getConfig().getKeyStore(), getConfig().getKeyStorePassword(), getConfig().getTrustStore(), getConfig().getTrustStorePassword()));
			client.setTracer(getTracer());

			CallbackConnection mqttConn = client.callbackConnection();
			mqttConn.connect(connectCallback);
			mqttConns.add(mqttConn);
		}

		while (connectionLatch.getCount() != 0) {
			setState("Waiting for " + connectionLatch.getCount() + " more connections to initialize");
			connectionLatch.await(10, TimeUnit.SECONDS);
		}
	}

	@Override
	protected void destroyMQTTConnections() {
		logger.info("Disconnecting MQTT connection");

		for (CallbackConnection c : mqttConns) {
			c.kill(null);
		}

		mqttConns.clear();
	}

	@Override
	protected void reply(byte[] payload, String mqttReplyTopic) throws InterruptedException {
		mqttConns.get(connIndex.getAndIncrement() % mqttConns.size()).publish(mqttReplyTopic, payload, getConfig().getQos(), false, publishCallback);
		getPublishedCount().incrementAndGet();
	}

}