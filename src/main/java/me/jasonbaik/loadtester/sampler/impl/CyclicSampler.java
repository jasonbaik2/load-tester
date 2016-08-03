package me.jasonbaik.loadtester.sampler.impl;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sampler.PayloadIterator;
import me.jasonbaik.loadtester.sampler.Sampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CyclicSampler extends Sampler<byte[], CyclicSamplerConfig> {

	private static final Logger logger = LogManager.getLogger(CyclicSampler.class);

	private ScheduledExecutorService es;

	public CyclicSampler(CyclicSamplerConfig config) {
		super(config);
	}

	@Override
	public void destroy() {
		if (this.es != null) {
			this.es.shutdownNow();
		}
	}

	@Override
	public void forEach(SamplerTask<byte[]> samplerTask, List<byte[]> payloads) throws InterruptedException {
		executeCycles(new FixedCountRunnable(samplerTask, payloads));
	}

	@Override
	public void forEach(SamplerTask<byte[]> samplerTask, PayloadIterator<byte[]> payloadIterator) throws InterruptedException {
		executeCycles(new FixedCountRunnable(samplerTask, payloadIterator));
	}

	private void executeCycles(FixedCountRunnable runnable) throws InterruptedException {
		logger.info("CyclicSampler will run the task with a " + getConfig().getInterval() + " " + getConfig().getIntervalUnit() + " interval");

		es = Executors.newSingleThreadScheduledExecutor();
		es.scheduleAtFixedRate(runnable, 0, getConfig().getInterval(), getConfig().getIntervalUnit());

		boolean terminated = false;

		while (!terminated) {
			try {
				terminated = es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				throw new InterruptedException("Cycle executor interrupted");
			}
		}
	}

	private class FixedCountRunnable implements Runnable {

		private int index = 0;
		private SamplerTask<byte[]> task;
		private List<byte[]> payloads;
		private PayloadIterator<byte[]> payloadIterator;

		private FixedCountRunnable(SamplerTask<byte[]> task, List<byte[]> payloads) {
			this.task = task;
			this.payloads = payloads;
		}

		private FixedCountRunnable(SamplerTask<byte[]> task, PayloadIterator<byte[]> payloadIterator) {
			this.task = task;
			this.payloadIterator = payloadIterator;
		}

		@Override
		public void run() {
			if (payloads != null) {
				if (index == payloads.size()) {
					logger.info("Sampler has reached the last sample. Shutting down the sampler...");
					es.shutdown();
					return;
				}

				try {
					task.run(index, payloads.get(index));
				} catch (Exception e) {
					logger.error("Failed to run the sample task for sample #" + index, e);
				}

			} else if (payloadIterator != null) {
				if (!payloadIterator.hasNext()) {
					logger.info("Sampler has reached the last sample. Shutting down the sampler...");
					es.shutdown();
					return;
				}

				try {
					task.run(index, payloadIterator.next());
				} catch (Exception e) {
					logger.error("Failed to run the sample task for sample #" + index, e);
				}
			}

			index++;
		}

	}

	@Override
	public void during(SamplerTask<byte[]> samplerTask, List<byte[]> payloads, long duration, TimeUnit unit) throws InterruptedException {
		executeCycles(new FixedDurationRunnable(samplerTask, payloads, System.nanoTime() + TimeUnit.NANOSECONDS.convert(duration, unit)), duration, unit);
	}

	@Override
	public void during(SamplerTask<byte[]> samplerTask, PayloadIterator<byte[]> payloadIterator, long duration, TimeUnit unit) throws InterruptedException {
		executeCycles(new FixedDurationRunnable(samplerTask, payloadIterator, System.nanoTime() + TimeUnit.NANOSECONDS.convert(duration, unit)), duration, unit);
	}

	private void executeCycles(FixedDurationRunnable runnable, long duration, TimeUnit unit) throws InterruptedException {
		logger.info("CyclicSampler will run the task for " + duration + " " + unit + " with a " + getConfig().getInterval() + " " + getConfig().getIntervalUnit() + " interval");

		es = Executors.newSingleThreadScheduledExecutor();
		es.scheduleAtFixedRate(runnable, 0, getConfig().getInterval(), getConfig().getIntervalUnit());

		boolean terminated = false;

		while (!terminated) {
			try {
				terminated = es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				throw new InterruptedException("Cycle executor interrupted");
			}
		}
	}

	private class FixedDurationRunnable implements Runnable {

		private int index = 0;
		private long endTime;
		private SamplerTask<byte[]> task;
		private List<byte[]> payloads;
		private PayloadIterator<byte[]> payloadIterator;

		private FixedDurationRunnable(SamplerTask<byte[]> task, List<byte[]> payloads, long endTime) {
			this.task = task;
			this.payloads = payloads;
			this.endTime = endTime;
		}

		private FixedDurationRunnable(SamplerTask<byte[]> task, PayloadIterator<byte[]> payloadIterator, long endTime) {
			this.task = task;
			this.payloadIterator = payloadIterator;
			this.endTime = endTime;
		}

		@Override
		public void run() {
			if (System.nanoTime() >= endTime) {
				logger.info("Sampler has reached the end of the duration. Shutting down the sampler...");
				es.shutdown();
				return;
			}

			if (payloadIterator != null) {
				try {
					task.run(index, payloadIterator.next());
				} catch (Exception e) {
					logger.error("Failed to run the sample task for sample #" + index, e);
				}
			} else if (payloads != null) {
				try {
					task.run(index, payloads.get(index % payloads.size()));
				} catch (Exception e) {
					logger.error("Failed to run the sample task for sample #" + index, e);
				}
			}

			index++;
		}

	}

}
