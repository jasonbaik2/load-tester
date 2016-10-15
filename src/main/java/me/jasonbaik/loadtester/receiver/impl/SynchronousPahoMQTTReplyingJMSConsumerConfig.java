package me.jasonbaik.loadtester.receiver.impl;

import java.util.List;
import java.util.Map;

import me.jasonbaik.loadtester.receiver.AbstractReceiverConfig;
import me.jasonbaik.loadtester.valueobject.Broker;

public class SynchronousPahoMQTTReplyingJMSConsumerConfig extends AbstractReceiverConfig<SynchronousPahoMQTTReplyingJMSConsumer> {

	private static final long serialVersionUID = 1L;

	private List<Broker> brokers;
	private boolean ssl;
	private String queue;
	private boolean cleanSession;
	private int qos;
	private int retryAttempts = Integer.MAX_VALUE;

	private Map<String, String> sslProperties;

	@Override
	public Class<SynchronousPahoMQTTReplyingJMSConsumer> getReceiverClass() {
		return SynchronousPahoMQTTReplyingJMSConsumer.class;
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
		return "SynchronousPahoMQTTReplyingJMSConsumerConfig [brokers=" + brokers + ", ssl=" + ssl + ", queue=" + queue + ", cleanSession=" + cleanSession + ", qos=" + qos + ", retryAttempts="
				+ retryAttempts + ", sslProperties=" + sslProperties + "]";
	}

}