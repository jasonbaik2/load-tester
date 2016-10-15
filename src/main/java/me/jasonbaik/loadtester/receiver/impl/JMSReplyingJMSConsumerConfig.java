package me.jasonbaik.loadtester.receiver.impl;

import java.util.List;

import me.jasonbaik.loadtester.receiver.AbstractReceiverConfig;
import me.jasonbaik.loadtester.valueobject.Broker;

public class JMSReplyingJMSConsumerConfig extends AbstractReceiverConfig<JMSReplyingJMSConsumer> {

	private static final long serialVersionUID = 1L;

	private List<Broker> brokers;
	private String queue;

	@Override
	public Class<JMSReplyingJMSConsumer> getReceiverClass() {
		return JMSReplyingJMSConsumer.class;
	}

	public String getQueue() {
		return queue;
	}

	public void setQueue(String queue) {
		this.queue = queue;
	}

	public List<Broker> getBrokers() {
		return brokers;
	}

	public void setBrokers(List<Broker> brokers) {
		this.brokers = brokers;
	}

	@Override
	public String toString() {
		return "JMSReplyingJMSConsumerConfig [brokers=" + brokers + ", queue=" + queue + "]";
	}

}