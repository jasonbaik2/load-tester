package me.jasonbaik.loadtester.sender.impl;

import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sender.SenderConfig;

import org.fusesource.mqtt.client.QoS;

public class ConnectionIncreasingMQTTPublisherConfig extends SenderConfig<ConnectionIncreasingMQTTPublisher> {

	private static final long serialVersionUID = 1L;

	private String name;

	private String mqttBroker;
	private String[] mqttBrokers;
	private String mqttBrokerUsername;
	private String mqttBrokerPassword;

	private String keyStore;
	private String keyStorePassword;
	private String trustStore;
	private String trustStorePassword;

	private int numConnections;
	private Integer numMessages;
	private int messageByteLength;
	private int messagePoolSize;

	private Long duration;
	private TimeUnit durationUnit;
	private String topic;
	private boolean cleanSession;
	private QoS qos;
	private long keepAliveIntervalMilli;

	private int connectionStepSize;
	private long connectionStepIntervalMilli;

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

	public int getNumConnections() {
		return numConnections;
	}

	public void setNumConnections(int numConnections) {
		this.numConnections = numConnections;
	}

	public Integer getNumMessages() {
		return numMessages;
	}

	public void setNumMessages(Integer numMessages) {
		this.numMessages = numMessages;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public QoS getQos() {
		return qos;
	}

	public void setQos(QoS qos) {
		this.qos = qos;
	}

	public long getKeepAliveIntervalMilli() {
		return keepAliveIntervalMilli;
	}

	public void setKeepAliveIntervalMilli(long keepAliveIntervalMilli) {
		this.keepAliveIntervalMilli = keepAliveIntervalMilli;
	}

	public int getConnectionStepSize() {
		return connectionStepSize;
	}

	public void setConnectionStepSize(int connectionStepSize) {
		this.connectionStepSize = connectionStepSize;
	}

	public long getConnectionStepIntervalMilli() {
		return connectionStepIntervalMilli;
	}

	public void setConnectionStepIntervalMilli(long connectionStepIntervalMilli) {
		this.connectionStepIntervalMilli = connectionStepIntervalMilli;
	}

	public int getMessageByteLength() {
		return messageByteLength;
	}

	public void setMessageByteLength(int messageByteLength) {
		this.messageByteLength = messageByteLength;
	}

	@Override
	public Class<ConnectionIncreasingMQTTPublisher> getSenderClass() {
		return ConnectionIncreasingMQTTPublisher.class;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TimeUnit getDurationUnit() {
		return durationUnit;
	}

	public void setDurationUnit(TimeUnit durationUnit) {
		this.durationUnit = durationUnit;
	}

	public int getMessagePoolSize() {
		return messagePoolSize;
	}

	public void setMessagePoolSize(int messagePoolSize) {
		this.messagePoolSize = messagePoolSize;
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

	@Override
	public String toString() {
		return "RoundRobinMQTTPublisherConfig [name=" + name + ", mqttBroker=" + mqttBroker + ", mqttBrokers=" + mqttBrokers + ", mqttBrokerUsername=" + mqttBrokerUsername + ", keyStore=" + keyStore
				+ ", trustStore=" + trustStore + ", numConnections=" + numConnections + ", numMessages=" + numMessages + ", messageByteLength=" + messageByteLength + ", messagePoolSize="
				+ messagePoolSize + ", duration=" + duration + ", durationUnit=" + durationUnit + ", topic=" + topic + ", cleanSession=" + cleanSession + ", qos=" + qos + ", keepAliveIntervalMilli="
				+ keepAliveIntervalMilli + ", connectionStepSize=" + connectionStepSize + ", connectionStepIntervalMilli=" + connectionStepIntervalMilli + "]";
	}

}