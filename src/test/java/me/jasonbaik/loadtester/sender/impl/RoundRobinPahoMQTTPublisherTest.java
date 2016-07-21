package me.jasonbaik.loadtester.sender.impl;

import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.inject.Inject;

import me.jasonbaik.loadtester.sender.impl.RoundRobinPahoMQTTPublisher;
import me.jasonbaik.loadtester.sender.impl.RoundRobinPahoMQTTPublisherConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:spring/test/context-test-local-paho.xml" })
public class RoundRobinPahoMQTTPublisherTest {

	private static final Logger logger = LogManager.getLogger(RoundRobinPahoMQTTPublisher.class);

	@Inject
	private RoundRobinPahoMQTTPublisherConfig config;

	private class ConnectCallback implements IMqttActionListener {

		private MqttAsyncClient client;
		private boolean connected;

		private ConnectCallback(MqttAsyncClient client) {
			super();
			this.client = client;
		}

		@Override
		public synchronized void onSuccess(IMqttToken asyncActionToken) {
			if (!connected) {
				connected = true;
			} else {
				logger.info("Reconnect success");
			}

			try {
				client.subscribe(client.getClientId().toString(), 2, null, subscribeCallback);
			} catch (MqttException e) {
				logger.error("Failed to subscribe", e);
			}
		}

		@Override
		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			logger.error("Failed to establish connection", exception);
		}

	};

	private IMqttActionListener subscribeCallback = new IMqttActionListener() {

		@Override
		public void onSuccess(IMqttToken asyncActionToken) {
			synchronized (RoundRobinPahoMQTTPublisherTest.this) {
				RoundRobinPahoMQTTPublisherTest.this.notify();
			}
		}

		@Override
		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			logger.error("Failed to subscribe", exception);
		}

	};

	private IMqttActionListener publishCallback = new IMqttActionListener() {

		@Override
		public void onSuccess(IMqttToken asyncActionToken) {
			logger.info("Published");
		}

		@Override
		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			logger.error(exception);
		}

	};

	@Ignore
	@Test
	public void testReconnect() throws NoSuchAlgorithmException, MqttException, InterruptedException {
		String[] brokers = new String[] { "ssl://localhost:1883", "ssl://localhost:1883", "ssl://localhost:1883", "ssl://localhost:1883", "ssl://localhost:1883" };

		final MqttAsyncClient mqttClient = new MqttAsyncClient(brokers[0], "test", new MemoryPersistence());

		final MqttConnectOptions options = new MqttConnectOptions();
		options.setServerURIs(brokers);
		options.setCleanSession(false);
		options.setUserName("admin");
		options.setPassword("admin".toCharArray());
		options.setKeepAliveInterval(0);

		Properties props = new Properties();
		props.putAll(config.getSslProperties());
		options.setSSLProperties(props);

		mqttClient.setCallback(new MqttCallback() {

			@Override
			public void connectionLost(Throwable cause) {
				logger.error("Connection lost");

				try {
					mqttClient.connect(options, null, new ConnectCallback(mqttClient));
				} catch (MqttSecurityException e) {
					logger.error("Failed to reconnect", e);
				} catch (MqttException e) {
					logger.error("Failed to reconnect", e);
				}
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				logger.info("Completed");
			}

		});
		mqttClient.connect(options, null, new ConnectCallback(mqttClient));

		synchronized (this) {
			this.wait();
		}

		while (true) {
			try {
				Thread.sleep(1000);
				mqttClient.publish("test", "asf".getBytes(), 2, false, null, publishCallback);

			} catch (MqttException e) {
				logger.error("Failed to publish", e);
			} catch (InterruptedException e) {
				logger.error("Interrupted", e);
			}
		}
	}

	@Test
	public void testRotateBroker() {
		String[] brokers = new String[] { "a", "b", "c", "d" };
		brokers = RoundRobinPahoMQTTPublisher.rotateBrokers(brokers);
		Assert.assertArrayEquals(new String[] { "b", "c", "d", "a" }, brokers);
		brokers = RoundRobinPahoMQTTPublisher.rotateBrokers(brokers);
		Assert.assertArrayEquals(new String[] { "c", "d", "a", "b" }, brokers);
		brokers = RoundRobinPahoMQTTPublisher.rotateBrokers(brokers);
		Assert.assertArrayEquals(new String[] { "d", "a", "b", "c" }, brokers);
		brokers = RoundRobinPahoMQTTPublisher.rotateBrokers(brokers);
		Assert.assertArrayEquals(new String[] { "a", "b", "c", "d" }, brokers);
	}

}
