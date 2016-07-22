package me.jasonbaik.loadtester.sender.impl;

import java.util.Map;

public class RoundRobinPahoMQTTPublisherConfig extends AbstractRoundRobinMQTTPublisherConfig<RoundRobinPahoMQTTPublisher> {

	private static final long serialVersionUID = 1L;

	private Map<String, String> sslProperties;
	private int qos;

	@Override
	public Class<RoundRobinPahoMQTTPublisher> getSenderClass() {
		return RoundRobinPahoMQTTPublisher.class;
	}

	public int getQos() {
		return qos;
	}

	public void setQos(int qos) {
		this.qos = qos;
	}

	public Map<String, String> getSslProperties() {
		return sslProperties;
	}

	public void setSslProperties(Map<String, String> sslProperties) {
		this.sslProperties = sslProperties;
	}

	@Override
	public String toString() {
		return "RoundRobinPahoMQTTPublisherConfig [sslProperties=" + sslProperties + ", qos=" + qos + ", getNumConnections()=" + getNumConnections() + ", getNumMessages()=" + getNumMessages()
				+ ", getDuration()=" + getDuration() + ", getTopic()=" + getTopic() + ", getKeepAliveIntervalMilli()=" + getKeepAliveIntervalMilli() + ", getConnectionStepSize()="
				+ getConnectionStepSize() + ", getConnectionStepIntervalMilli()=" + getConnectionStepIntervalMilli() + ", getMessageByteLength()=" + getMessageByteLength() + ", getName()="
				+ getName() + ", getDurationUnit()=" + getDurationUnit() + ", getMessagePoolSize()=" + getMessagePoolSize() + ", getKeyStore()=" + getKeyStore() + ", getKeyStorePassword()="
				+ getKeyStorePassword() + ", getTrustStore()=" + getTrustStore() + ", getTrustStorePassword()=" + getTrustStorePassword() + ", isCleanSession()=" + isCleanSession()
				+ ", getBrokers()=" + getBrokers() + ", isSsl()=" + isSsl() + "]";
	}

}