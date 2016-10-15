package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.jasonbaik.loadtester.sampler.AbstractFixedCountSampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;

public class FixedCountRandomSampler extends AbstractFixedCountSampler<byte[], FixedCountRandomSamplerConfig> {

	private static final Logger logger = LogManager.getLogger(FixedCountRandomSampler.class);

	private Random random = new Random(System.nanoTime());

	private volatile long startTime;

	private DelayQueue<RandomlyDelayed> dq = new DelayQueue<RandomlyDelayed>();

	@Override
	public void destroy() {
		dq.clear();
	}

	private long randomDelay(long expected, TimeUnit unit) {
		return random.nextInt((int) TimeUnit.MILLISECONDS.convert(expected, unit)) << 1;
	}

	@Override
	public void forEach(SamplerTask<byte[]> samplerTask, List<byte[]> payloads) throws InterruptedException {
		logger.info("Generating random delays for " + payloads.size() + " payloads with an expected value of " + getConfig().getExpectedInterval() + " " + getConfig().getExpectedIntervalUnit());

		long cumulativeDelay = 0;

		for (int i = 0; i < payloads.size(); i++) {
			long delay = randomDelay(getConfig().getExpectedInterval(), getConfig().getExpectedIntervalUnit());
			cumulativeDelay += delay;
			dq.add(new RandomlyDelayed(cumulativeDelay));
		}

		logger.info(dq.size() + " random delays generated");

		startTime = System.currentTimeMillis();

		for (int index = 0; !dq.isEmpty();) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Sampler thread interrupted");
			}

			try {
				dq.take();

				logger.debug("Running the task for sample#" + index);
				samplerTask.run(index, payloads.get(index));
				index++;

			} catch (Exception e) {
				logger.error("Failed to run the task for sample #" + index, e);
			}
		}
	}

	@Override
	public void forEach(SamplerTask<byte[]> samplerTask, Iterator<byte[]> payloadGenerator) throws InterruptedException {
		logger.info("Running the task with random delays with an expected value of " + getConfig().getExpectedInterval() + " " + getConfig().getExpectedIntervalUnit());

		long cumulativeDelay = 0;
		startTime = System.currentTimeMillis();

		for (int index = 0; payloadGenerator.hasNext();) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Sampler thread interrupted");
			}

			long delay = randomDelay(getConfig().getExpectedInterval(), getConfig().getExpectedIntervalUnit());
			cumulativeDelay += delay;
			dq.add(new RandomlyDelayed(cumulativeDelay));

			try {
				dq.take();

				logger.debug("Running the task for sample#" + index);
				samplerTask.run(index, payloadGenerator.next());
				index++;

			} catch (Exception e) {
				logger.error("Failed to run the task for sample #" + index, e);
			}
		}
	}

	private final class RandomlyDelayed implements Delayed {

		private long cumulativeDelay;

		public RandomlyDelayed(long cumulativeDelay) {
			this.cumulativeDelay = cumulativeDelay;
		}

		@Override
		public int compareTo(Delayed o) {
			RandomlyDelayed other = (RandomlyDelayed) o;

			if (cumulativeDelay < other.cumulativeDelay) {
				return -1;
			} else if (cumulativeDelay == other.cumulativeDelay) {
				return 0;
			} else {
				return 1;
			}
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(startTime + cumulativeDelay - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		}
	}

}
