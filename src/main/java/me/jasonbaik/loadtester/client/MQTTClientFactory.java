package me.jasonbaik.loadtester.client;

import java.net.URISyntaxException;
import java.util.Properties;

import me.jasonbaik.loadtester.reporter.impl.MQTTFlightTracer;
import me.jasonbaik.loadtester.util.SSLUtil;
import me.jasonbaik.loadtester.valueobject.Broker;
import me.jasonbaik.loadtester.valueobject.Protocol;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.fusesource.mqtt.client.MQTT;

public class MQTTClientFactory {

	public enum ClientType {
		FUSESOURCE, PAHO;
	}

	public static MQTTClientWrapper createFuseSourceMQTTClient(Broker broker, String connectionId, boolean ssl, boolean cleanSession, long keepAlive, MQTTFlightTracer tracer) {
		MQTT client = new MQTT();

		try {
			client.setHost(MQTTClientFactory.getFusesourceConnectionUrl(broker, ssl));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		client.setClientId(connectionId);
		client.setCleanSession(cleanSession);
		client.setUserName(broker.getUsername());
		client.setPassword(broker.getPassword());
		client.setKeepAlive((short) keepAlive);

		if (ssl) {
			try {
				client.setSslContext(SSLUtil.createSSLContext(broker.getKeyStore(), broker.getKeyStorePassword(), broker.getTrustStore(), broker.getTrustStorePassword()));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		if (tracer != null) {
			client.setTracer(tracer);
		}

		return new FuseSourceMQTTClientAdapter(client);
	}

	public static MQTTClientWrapper createPahoMQTTClient(Broker broker, String connectionId, boolean ssl, boolean cleanSession, long keepAlive) throws MqttException {
		MqttAsyncClient mqttClient = new MqttAsyncClient(MQTTClientFactory.getPahoConnectionUrl(broker, ssl), connectionId, new MemoryPersistence());
		MqttConnectOptions options = new MqttConnectOptions();

		options.setCleanSession(cleanSession);
		options.setUserName(broker.getUsername());
		options.setPassword(broker.getPassword().toCharArray());
		options.setKeepAliveInterval((int) keepAlive);

		Properties props = new Properties();
		props.putAll(broker.getSslProperties());
		options.setSSLProperties(props);

		return new PahoMQTTClientAdapter(mqttClient, options);
	}

	public static MQTTClientWrapper createPahoMQTTClient() {
		return null;
	}

	public static String getFusesourceConnectionUrl(Broker broker, boolean ssl) {
		if (ssl) {
			return "tlsv1.2://" + broker.getHostname() + ":" + broker.getConnectors().get(Protocol.MQTT).getPort() + "?transport.enabledCipherSuites=TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256";
		} else {
			return "tcp://" + broker.getHostname() + ":" + broker.getConnectors().get(Protocol.MQTT).getPort();
		}
	}

	public static String getPahoConnectionUrl(Broker broker, boolean ssl) {
		if (ssl) {
			return "ssl://" + broker.getHostname() + ":" + broker.getConnectors().get(Protocol.MQTT).getPort();
		} else {
			return "tcp://" + broker.getHostname() + ":" + broker.getConnectors().get(Protocol.MQTT).getPort();
		}
	}

}
