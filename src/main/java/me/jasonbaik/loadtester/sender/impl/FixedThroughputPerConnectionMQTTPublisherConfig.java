package me.jasonbaik.loadtester.sender.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sender.SenderConfig;
import me.jasonbaik.loadtester.valueobject.Broker;

import org.fusesource.mqtt.client.QoS;

public class FixedThroughputPerConnectionMQTTPublisherConfig extends SenderConfig<ConnectionIncreasingMQTTPublisher> {

	private static final long serialVersionUID = 1L;

	private String name;

	private List<Broker> brokers;
	private boolean ssl;
	private boolean trace;

	private String keyStore;
	private String keyStorePassword;
	private String trustStore;
	private String trustStorePassword;

	private int messageByteLength;
	private int messagePoolSize;
	private QoS qos;

	private String topic;
	private boolean cleanSession;
	private long keepAliveIntervalMilli;

	private int numConnections;
	private int newConnectionInterval;
	private TimeUnit newConnectionIntervalUnit;
	private int connectionStepSize;

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

	public List<Broker> getBrokers() {
		return brokers;
	}

	public void setBrokers(List<Broker> brokers) {
		this.brokers = brokers;
	}

	public boolean isSsl() {
		return ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public String getKeyStore() {
		return keyStore;
	}

	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}

	public String getTrustStore() {
		return trustStore;
	}

	public void setTrustStore(String trustStore) {
		this.trustStore = trustStore;
	}

	public int getMessageByteLength() {
		return messageByteLength;
	}

	public void setMessageByteLength(int messageByteLength) {
		this.messageByteLength = messageByteLength;
	}

	public int getMessagePoolSize() {
		return messagePoolSize;
	}

	public void setMessagePoolSize(int messagePoolSize) {
		this.messagePoolSize = messagePoolSize;
	}

	public QoS getQos() {
		return qos;
	}

	public void setQos(QoS qos) {
		this.qos = qos;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public long getKeepAliveIntervalMilli() {
		return keepAliveIntervalMilli;
	}

	public void setKeepAliveIntervalMilli(long keepAliveIntervalMilli) {
		this.keepAliveIntervalMilli = keepAliveIntervalMilli;
	}

	public int getNewConnectionInterval() {
		return newConnectionInterval;
	}

	public void setNewConnectionInterval(int newConnectionInterval) {
		this.newConnectionInterval = newConnectionInterval;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public int getConnectionStepSize() {
		return connectionStepSize;
	}

	public void setConnectionStepSize(int connectionStepSize) {
		this.connectionStepSize = connectionStepSize;
	}

	public int getNumConnections() {
		return numConnections;
	}

	public void setNumConnections(int numConnections) {
		this.numConnections = numConnections;
	}

	public TimeUnit getNewConnectionIntervalUnit() {
		return newConnectionIntervalUnit;
	}

	public void setNewConnectionIntervalUnit(TimeUnit newConnectionIntervalUnit) {
		this.newConnectionIntervalUnit = newConnectionIntervalUnit;
	}

	public boolean isTrace() {
		return trace;
	}

	public void setTrace(boolean trace) {
		this.trace = trace;
	}

	@Override
	public String toString() {
		return "ConnectionIncreasingMQTTPublisherConfig [name=" + name + ", brokers=" + brokers + ", ssl=" + ssl + ", trace=" + trace + ", keyStore=" + keyStore + ", keyStorePassword="
				+ keyStorePassword + ", trustStore=" + trustStore + ", trustStorePassword=" + trustStorePassword + ", messageByteLength=" + messageByteLength + ", messagePoolSize=" + messagePoolSize
				+ ", qos=" + qos + ", topic=" + topic + ", cleanSession=" + cleanSession + ", keepAliveIntervalMilli=" + keepAliveIntervalMilli + ", numConnections=" + numConnections
				+ ", newConnectionInterval=" + newConnectionInterval + ", newConnectionIntervalUnit=" + newConnectionIntervalUnit + ", connectionStepSize=" + connectionStepSize + "]";
	}

}