package me.jasonbaik.loadtester.sender.impl;

import org.fusesource.mqtt.client.QoS;

public class RoundRobinMQTTPublisherConfig extends AbstractRoundRobinMQTTPublisherConfig<RoundRobinMQTTPublisher> {

	private static final long serialVersionUID = 1L;

	private QoS qos;

	@Override
	public Class<RoundRobinMQTTPublisher> getSenderClass() {
		return RoundRobinMQTTPublisher.class;
	}

	public QoS getQos() {
		return qos;
	}

	public void setQos(QoS qos) {
		this.qos = qos;
	}

	@Override
	public String toString() {
		return "RoundRobinMQTTPublisherConfig [qos=" + qos + ", getNumConnections()=" + getNumConnections() + ", getNumMessages()=" + getNumMessages() + ", getDuration()=" + getDuration()
				+ ", getTopic()=" + getTopic() + ", getKeepAliveIntervalMilli()=" + getKeepAliveIntervalMilli() + ", getConnectionStepSize()=" + getConnectionStepSize()
				+ ", getConnectionStepIntervalMilli()=" + getConnectionStepIntervalMilli() + ", getMessageByteLength()=" + getMessageByteLength() + ", getName()=" + getName() + ", getDurationUnit()="
				+ getDurationUnit() + ", getMessagePoolSize()=" + getMessagePoolSize() + ", getKeyStore()=" + getKeyStore() + ", getKeyStorePassword()=" + getKeyStorePassword() + ", getTrustStore()="
				+ getTrustStore() + ", getTrustStorePassword()=" + getTrustStorePassword() + ", isCleanSession()=" + isCleanSession() + ", getBrokers()=" + getBrokers() + ", isSsl()=" + isSsl() + "]";
	}

}