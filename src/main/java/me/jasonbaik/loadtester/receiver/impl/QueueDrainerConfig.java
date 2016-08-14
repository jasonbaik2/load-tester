package me.jasonbaik.loadtester.receiver.impl;

import java.util.List;

import me.jasonbaik.loadtester.receiver.ReceiverConfig;
import me.jasonbaik.loadtester.valueobject.Broker;

public class QueueDrainerConfig extends ReceiverConfig<QueueDrainer> {

	private static final long serialVersionUID = 1L;

	private List<Broker> brokers;
	private String queue;
	private int numJMSConnections = 1;

	@Override
	public Class<QueueDrainer> getReceiverClass() {
		// TODO Auto-generated method stub
		return QueueDrainer.class;
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