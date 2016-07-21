package me.jasonbaik.loadtester.receiver.impl;

import java.util.Map;

import me.jasonbaik.loadtester.receiver.ReceiverConfig;

public class SynchronousPahoMQTTReplyingJMSConsumerConfig extends ReceiverConfig<SynchronousPahoMQTTReplyingJMSConsumer> {

	private static final long serialVersionUID = 1L;

	private String jmsBroker;
	private String jmsBrokerUsername;
	private String jmsBrokerPassword;

	private String queue;

	private String mqttBroker;
	private String[] mqttBrokers;
	private String mqttBrokerUsername;
	private String mqttBrokerPassword;
	private boolean cleanSession;
	private int qos;
	private int retryAttempts = Integer.MAX_VALUE;

	private Map<String, String> sslProperties;

	@Override
	public Class<SynchronousPahoMQTTReplyingJMSConsumer> getReceiverClass() {
		return SynchronousPahoMQTTReplyingJMSConsumer.class;
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

	public String[] getMqttBrokers() {
		return mqttBrokers;
	}

	public void setMqttBrokers(String[] mqttBrokers) {
		this.mqttBrokers = mqttBrokers;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public int getRetryAttempts() {
		return retryAttempts;
	}

	public void setRetryAttempts(int retryAttempts) {
		this.retryAttempts = retryAttempts;
	}

	@Override
	public String toString() {
		return "SynchronousPahoMQTTReplyingJMSConsumerConfig [jmsBroker=" + jmsBroker + ", jmsBrokerUsername=" + jmsBrokerUsername + ", queue=" + queue + ", mqttBroker=" + mqttBroker
				+ ", mqttBrokers=" + mqttBrokers + ", mqttBrokerUsername=" + mqttBrokerUsername + ", cleanSession=" + cleanSession + ", qos=" + qos + ", sslProperties=" + sslProperties + "]";
	}

}