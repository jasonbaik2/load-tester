package me.jasonbaik.loadtester.client;

import me.jasonbaik.loadtester.valueobject.Broker;
import me.jasonbaik.loadtester.valueobject.Protocol;

public class MQTTClientFactory {

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
