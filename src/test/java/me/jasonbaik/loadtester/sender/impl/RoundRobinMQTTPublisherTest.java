package me.jasonbaik.loadtester.sender.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.net.ssl.SSLContext;

import me.jasonbaik.loadtester.sender.impl.RoundRobinMQTTPublisherConfig;
import me.jasonbaik.loadtester.util.SSLUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.MQTT;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:spring/test/context-test-local.xml" })
public class RoundRobinMQTTPublisherTest {

	private static final Logger logger = LogManager.getLogger(RoundRobinMQTTPublisherTest.class);

	@Inject
	private ConnectionFactory connFactory;

	@Inject
	private RoundRobinMQTTPublisherConfig config;

	@Ignore
	@Test
	public void testSSL() throws URISyntaxException, InterruptedException, JMSException, NoSuchAlgorithmException, KeyManagementException, CertificateException, FileNotFoundException, IOException,
			KeyStoreException, UnrecoverableKeyException {
		// Listen for connection advisory messages
		Connection jmsConn = connFactory.createConnection();
		Session session = jmsConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		MessageConsumer consumer = session.createConsumer(session.createTopic("ActiveMQ.Advisory.Connection"));
		consumer.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(Message message) {
				logger.info(message);
			}

		});

		jmsConn.start();

		// Try an SSL MQTT connection
		MQTT client = new MQTT();
		client.setHost(config.getMqttBroker());
		client.setClientId("ssl-test");
		client.setCleanSession(true);
		client.setUserName(config.getMqttBrokerUsername());
		client.setPassword(config.getMqttBrokerPassword());
		client.setKeepAlive((short) (config.getKeepAliveIntervalMilli() / 1000));
		client.setConnectAttemptsMax(1);
		client.setReconnectAttemptsMax(1);
		client.setSslContext(SSLUtil.createSSLContext(getConfig().getKeyStore(), getConfig().getKeyStorePassword(), getConfig().getTrustStore(), getConfig().getTrustStorePassword()));

		System.out.println(Arrays.asList(SSLContext.getDefault().createSSLEngine().getSupportedCipherSuites()).toString());

		CallbackConnection conn = client.callbackConnection();
		conn.connect(new Callback<Void>() {

			@Override
			public void onSuccess(Void value) {
				logger.info("Successfully established an SSL MQTT connection");
				Assert.assertTrue(true);

				synchronized (RoundRobinMQTTPublisherTest.this) {
					RoundRobinMQTTPublisherTest.this.notify();
				}
			}

			@Override
			public void onFailure(Throwable value) {
				logger.info("Failed to establish an SSL MQTT connection", value);
				Assert.fail("SSL connection failed");

				synchronized (RoundRobinMQTTPublisherTest.this) {
					RoundRobinMQTTPublisherTest.this.notify();
				}
			}

		});

		synchronized (this) {
			this.wait();
		}
	}

	public RoundRobinMQTTPublisherConfig getConfig() {
		return config;
	}

	public void setConfig(RoundRobinMQTTPublisherConfig config) {
		this.config = config;
	}

	public ConnectionFactory getConnFactory() {
		return connFactory;
	}

	public void setConnFactory(ConnectionFactory connFactory) {
		this.connFactory = connFactory;
	}

}
