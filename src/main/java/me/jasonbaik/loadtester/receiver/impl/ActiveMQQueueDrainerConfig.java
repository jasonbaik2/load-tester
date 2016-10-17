package me.jasonbaik.loadtester.receiver.impl;

import java.util.List;

import me.jasonbaik.loadtester.receiver.AbstractReceiverConfig;
import me.jasonbaik.loadtester.valueobject.Broker;

public class ActiveMQQueueDrainerConfig extends AbstractReceiverConfig<ActiveMQQueueDrainer> {

	private static final long serialVersionUID = 1L;

	private List<Broker> brokers;
	private String queue;
	private int numJMSConnections = 1;

	@Override
	public Class<ActiveMQQueueDrainer> getReceiverClass() {
		// TODO Auto-generated method stub
		return ActiveMQQueueDrainer.class;
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

	public int getNumJMSConnections() {
		return numJMSConnections;
	}

	public void setNumJMSConnections(int numJMSConnections) {
		this.numJMSConnections = numJMSConnections;
	}

	@Override
	public String toString() {
		return "QueueDrainerConfig [brokers=" + brokers + ", queue=" + queue + ", numJMSConnections=" + numJMSConnections + "]";
	}

}