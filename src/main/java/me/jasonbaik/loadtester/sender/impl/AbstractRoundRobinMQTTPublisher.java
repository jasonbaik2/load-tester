package me.jasonbaik.loadtester.sender.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import me.jasonbaik.loadtester.sampler.PayloadIterator;
import me.jasonbaik.loadtester.sampler.Sampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;
import me.jasonbaik.loadtester.sender.Sender;
import me.jasonbaik.loadtester.util.RandomXmlGenerator;
import me.jasonbaik.loadtester.valueobject.Broker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractRoundRobinMQTTPublisher<T extends AbstractRoundRobinMQTTPublisherConfig<? extends AbstractRoundRobinMQTTPublisher<T>>> extends Sender<byte[], T> {

	private static final Logger logger = LogManager.getLogger(AbstractRoundRobinMQTTPublisher.class);

	private List<byte[]> payloads;

	private AtomicInteger clientIndex = new AtomicInteger(0);
	private AtomicInteger publishedCount = new AtomicInteger(0);
	private AtomicInteger successCount = new AtomicInteger(0);
	private AtomicInteger failureCount = new AtomicInteger(0);
	private AtomicInteger repliedCount = new AtomicInteger(0);

	private volatile boolean pubDone;

	private int brokerIndex = 0;

	public AbstractRoundRobinMQTTPublisher(T config) {
		super(config);
		// TODO Auto-generated constructor stub
	}

	protected Broker getNextBroker() {
		return getConfig().getBrokers().get(brokerIndex++ % getConfig().getBrokers().size());
	}

	@Override
	public void init() throws Exception {
		logger.info("Pre-generating a pool of " + getConfig().getMessagePoolSize() + " random payloads...");

		payloads = new ArrayList<byte[]>(getConfig().getMessagePoolSize());

		for (int i = 0; i < getConfig().getMessagePoolSize(); i++) {
			payloads.add(RandomXmlGenerator.generate(getConfig().getMessageByteLength()));
		}
	}

	@Override
	public void send(Sampler<byte[], ?> sampler) throws Exception {
		SamplerTask<byte[]> task = new SamplerTask<byte[]>() {

			@Override
			public void run(int index, byte[] payload) throws Exception {
				roundRobinSend(index, payload);
			}

		};

		if (getConfig().getDuration() != null) {
			logger.info("Publishing messages for " + getConfig().getDuration() + " " + getConfig().getDurationUnit());

			PayloadIterator<byte[]> payloadIterator = new PayloadIterator<byte[]>() {

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public byte[] next() {
					return payloads.get(publishedCount.get() % payloads.size());
				}

				@Override
				public boolean hasNext() {
					return true;
				}

			};

			sampler.during(task, payloadIterator, getConfig().getDuration(), getConfig().getDurationUnit());
			pubDone = true;

		} else if (getConfig().getNumMessages() != null) {
			logger.info("Publishing " + getConfig().getNumMessages() + " messages...");

			PayloadIterator<byte[]> payloadIterator = new PayloadIterator<byte[]>() {

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public byte[] next() {
					return payloads.get(publishedCount.get() % payloads.size());
				}

				@Override
				public boolean hasNext() {
					if (publishedCount.get() == getConfig().getNumMessages()) {
						return false;
					}
					return true;
				}

			};

			sampler.forEach(task, payloadIterator);
			pubDone = true;
		}
	}

	protected abstract void roundRobinSend(int index, byte[] payload) throws Exception;

	public List<byte[]> getPayloads() {
		return payloads;
	}

	public void setPayloads(List<byte[]> payloads) {
		this.payloads = payloads;
	}

	public AtomicInteger getClientIndex() {
		return clientIndex;
	}

	public void setClientIndex(AtomicInteger clientIndex) {
		this.clientIndex = clientIndex;
	}

	public AtomicInteger getPublishedCount() {
		return publishedCount;
	}

	public void setPublishedCount(AtomicInteger publishedCount) {
		this.publishedCount = publishedCount;
	}

	public AtomicInteger getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(AtomicInteger successCount) {
		this.successCount = successCount;
	}

	public AtomicInteger getFailureCount() {
		return failureCount;
	}

	public void setFailureCount(AtomicInteger failureCount) {
		this.failureCount = failureCount;
	}

	public AtomicInteger getRepliedCount() {
		return repliedCount;
	}

	public void setRepliedCount(AtomicInteger repliedCount) {
		this.repliedCount = repliedCount;
	}

	public boolean isPubDone() {
		return pubDone;
	}

	public void setPubDone(boolean pubDone) {
		this.pubDone = pubDone;
	}

	public int getBrokerIndex() {
		return brokerIndex;
	}

	public void setBrokerIndex(int brokerIndex) {
		this.brokerIndex = brokerIndex;
	}

}