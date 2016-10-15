package me.jasonbaik.loadtester.receiver.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.receiver.AbstractReceiverConfig;
import me.jasonbaik.loadtester.valueobject.Broker;

public class BrokerThreadCountCollectorConfig extends AbstractReceiverConfig<BrokerThreadCountCollector> {

	private static final long serialVersionUID = 1L;

	private List<Broker> brokers;
	private long collectInterval;
	private TimeUnit collectIntervalUnit;

	@Override
	public Class<BrokerThreadCountCollector> getReceiverClass() {
		return BrokerThreadCountCollector.class;
	}

	public List<Broker> getBrokers() {
		return brokers;
	}

	public void setBrokers(List<Broker> brokers) {
		this.brokers = brokers;
	}

	public long getCollectInterval() {
		return collectInterval;
	}

	public void setCollectInterval(long collectInterval) {
		this.collectInterval = collectInterval;
	}

	public TimeUnit getCollectIntervalUnit() {
		return collectIntervalUnit;
	}

	public void setCollectIntervalUnit(TimeUnit collectIntervalUnit) {
		this.collectIntervalUnit = collectIntervalUnit;
	}

}