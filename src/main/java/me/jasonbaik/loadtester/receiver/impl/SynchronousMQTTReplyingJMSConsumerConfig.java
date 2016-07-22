package me.jasonbaik.loadtester.receiver.impl;

import java.util.List;

import me.jasonbaik.loadtester.receiver.ReceiverConfig;
import me.jasonbaik.loadtester.valueobject.Broker;

import org.fusesource.mqtt.client.QoS;

public class SynchronousMQTTReplyingJMSConsumerConfig extends ReceiverConfig<SynchronousMQTTReplyingJMSConsumer> {

	private static final long serialVersionUID = 1L;

	private List<Broker> brokers;
	private boolean ssl;
	private String queue;
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

	@Override
	public String toString() {
		return "SynchronousMQTTReplyingJMSConsumerConfig [brokers=" + brokers + ", ssl=" + ssl + ", queue=" + queue + ", cleanSession=" + cleanSession + ", qos=" + qos + ", keyStore=" + keyStore
				+ ", trustStore=" + trustStore + "]";
	}

}