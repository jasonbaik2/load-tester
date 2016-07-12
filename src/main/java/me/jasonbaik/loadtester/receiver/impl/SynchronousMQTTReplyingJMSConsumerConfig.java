package me.jasonbaik.loadtester.receiver.impl;

import me.jasonbaik.loadtester.receiver.ReceiverConfig;

import org.fusesource.mqtt.client.QoS;

public class SynchronousMQTTReplyingJMSConsumerConfig extends ReceiverConfig<SynchronousMQTTReplyingJMSConsumer> {

	private static final long serialVersionUID = 1L;

	private String jmsBroker;
	private String jmsBrokerUsername;
	private String jmsBrokerPassword;

	private String queue;

	private String mqttBroker;
	private String mqttBrokerUsername;
	private String mqttBrokerPassword;
	private boolean cleanSession;
	private QoS qos;

	private String keyStore;
	private String keyStorePassword;
	private String trustStore;
	private String trustStorePassword;

	@Override
	public Class<SynchronousMQTTReplyingJMSConsumer> getReceiverClass() {
		return SynchronousMQTTReplyingJMSConsumer.class;
	}

	public String getJmsBroker() {
		return jmsBroker;
	}

	public void setJmsBroker(String jmsBroker) {
		this.jmsBroker = jmsBroker;
	}

	public String getJmsBrokerUsername() {
		return jmsBrokerUsername;
	}

	public void setJmsBrokerUsername(String jmsBrokerUsername) {
		this.jmsBrokerUsername = jmsBrokerUsername;
	}

	public String getJmsBrokerPassword() {
		return jmsBrokerPassword;
	}

	public void setJmsBrokerPassword(String jmsBrokerPassword) {
		this.jmsBrokerPassword = jmsBrokerPassword;
	}

	public String getMqttBroker() {
		return mqttBroker;
	}

	public void setMqttBroker(String mqttBroker) {
		this.mqttBroker = mqttBroker;
	}

	public String getMqttBrokerUsername() {
		return mqttBrokerUsername;
	}

	public void setMqttBrokerUsername(String mqttBrokerUsername) {
		this.mqttBrokerUsername = mqttBrokerUsername;
	}

	public String getMqttBrokerPassword() {
		return mqttBrokerPassword;
	}

	public void setMqttBrokerPassword(String mqttBrokerPassword) {
		this.mqttBrokerPassword = mqttBrokerPassword;
	}

	public String getQueue() {
		return queue;
	}

	public void setQueue(String queue) {
		this.queue = queue;
	}

	public QoS getQos() {
		return qos;
	}

	public void setQos(QoS qos) {
		this.qos = qos;
	}

	public String getKeyStore() {
		return keyStore;
	}

	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getTrustStore() {
		return trustStore;
	}

	public void setTrustStore(String trustStore) {
		this.trustStore = trustStore;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	@Override
	public String toString() {
		return "SynchronousMQTTReplyingJMSConsumerConfig [jmsBroker=" + jmsBroker + ", jmsBrokerUsername=" + jmsBrokerUsername + ", queue=" + queue + ", mqttBroker=" + mqttBroker
				+ ", mqttBrokerUsername=" + mqttBrokerUsername + ", cleanSession=" + cleanSession + ", qos=" + qos + ", keyStore=" + keyStore + ", trustStore=" + trustStore + "]";
	}

}