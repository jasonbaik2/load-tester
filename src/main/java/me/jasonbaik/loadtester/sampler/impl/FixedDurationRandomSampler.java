package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.jasonbaik.loadtester.sampler.AbstractFixedDurationSampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;

public class FixedDurationRandomSampler extends AbstractFixedDurationSampler<byte[], FixedDurationRandomSamplerConfig> {

	private static final Logger logger = LogManager.getLogger(FixedDurationRandomSampler.class);

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

	/**
	 * The method assumes that the # of payloads is high enough (not exhausted) to support the specified duration
	 * 
	 * @param samplerTask
	 * @param payloads
	 * @param duration
	 * @param unit
	 * @throws InterruptedException
	 */
	@Override
	protected void during(SamplerTask<byte[]> samplerTask, List<byte[]> payloads) throws InterruptedException {
		generateRandomDelays(getConfig().getDuration(), getConfig().getUnit());

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

		logger.info(getClass().getName() + " has finished running the task for all samples");
	}

	/**
	 * The method assumes that the # of payloads is high enough (not exhausted) to support the specified duration
	 * 
	 * @param samplerTask
	 * @param payloads
	 * @param duration
	 * @param unit
	 * @throws InterruptedException
	 */
	@Override
	protected void during(SamplerTask<byte[]> samplerTask, Iterator<byte[]> payloadGenerator) throws InterruptedException {
		generateRandomDelays(getConfig().getDuration(), getConfig().getUnit());

		startTime = System.currentTimeMillis();

		for (int index = 0; !dq.isEmpty();) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Sampler thread interrupted");
			}

			try {
				dq.take();

				logger.debug("Running the task for sample#" + index);
				samplerTask.run(index, payloadGenerator.next());
				index++;

			} catch (Exception e) {
				logger.error("Failed to run the task for sample #" + index, e);
			}
		}

		logger.info(getClass().getName() + " has finished running the task for all samples");
	}

	private void generateRandomDelays(long duration, TimeUnit unit) {
		logger.info("Generating random delays that sum to " + duration + " " + unit);

		int cumulativeDelay = 0;
		long durationMillis = TimeUnit.MILLISECONDS.convert(duration, unit);

		while (cumulativeDelay < durationMillis) {
			long delay = randomDelay(getConfig().getExpectedInterval(), getConfig().getExpectedIntervalUnit());
			cumulativeDelay += delay;
			dq.add(new RandomlyDelayed(cumulativeDelay));
		}

		logger.info(dq.size() + " random delays generated");
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
