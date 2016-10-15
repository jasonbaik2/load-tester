package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.jasonbaik.loadtester.sampler.AbstractFixedDurationSampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;

public class FixedDurationBurstSampler extends AbstractFixedDurationSampler<byte[], FixedDurationBurstSamplerConfig> {

	private static final Logger logger = LogManager.getLogger(FixedDurationBurstSampler.class);

	private ScheduledExecutorService es;

	@Override
	public void destroy() {
		if (this.es != null) {
			this.es.shutdownNow();
		}
	}

	@Override
	public void during(SamplerTask<byte[]> sampleTask, List<byte[]> payloads) throws InterruptedException {
		executeBursts(new BurstRunnable(sampleTask, payloads, System.nanoTime() + TimeUnit.NANOSECONDS.convert(getConfig().getDuration(), getConfig().getUnit())), getConfig().getDuration(),
				getConfig().getUnit());
	}

	@Override
	public void during(SamplerTask<byte[]> sampleTask, Iterator<byte[]> payloadGenerator) throws InterruptedException {
		executeBursts(new BurstRunnable(sampleTask, payloadGenerator, System.nanoTime() + TimeUnit.NANOSECONDS.convert(getConfig().getDuration(), getConfig().getUnit())), getConfig().getDuration(),
				getConfig().getUnit());
	}

	private void executeBursts(BurstRunnable burstRunnable, long duration, TimeUnit unit) throws InterruptedException {
		logger.info(getClass().getName() + " will run the task every " + getConfig().getBurstInterval() + " " + getConfig().getBurstIntervalUnit() + " for " + duration + " " + unit);

		es = Executors.newSingleThreadScheduledExecutor();
		es.scheduleAtFixedRate(burstRunnable, 0, getConfig().getBurstInterval(), getConfig().getBurstIntervalUnit());

		boolean terminated = false;

		while (!terminated) {
			try {
				terminated = es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				throw new InterruptedException("Burst executor interrupted");
			}
		}
	}

	private class BurstRunnable implements Runnable {

		private long endTime;
		private SamplerTask<byte[]> task;
		private List<byte[]> payloads;
		private Iterator<byte[]> payloadGenerator;

		private BurstRunnable(SamplerTask<byte[]> task, List<byte[]> payloads, long endTime) {
			this.task = task;
			this.payloads = payloads;
			this.endTime = endTime;
		}

		private BurstRunnable(SamplerTask<byte[]> task, Iterator<byte[]> payloadGenerator, long endTime) {
			this.task = task;
			this.payloadGenerator = payloadGenerator;
			this.endTime = endTime;
		}

		@Override
		public void run() {
			if (System.nanoTime() >= endTime) {
				logger.info("Sampler has reached the end of the duration. Shutting down the sampler...");
				es.shutdown();
				return;
			}

			try {
				if (this.payloadGenerator != null) {
					forEach(task, payloadGenerator);
				} else if (this.payloads != null) {
					forEach(task, payloads);
				}
			} catch (InterruptedException e) {
				logger.warn("Burst executor interrupted", e);
			}
		}

		protected void forEach(SamplerTask<byte[]> samplerTask, List<byte[]> payloads) throws InterruptedException {
			logger.info(getClass().getName() + " will run the task " + getConfig().getBurstCount() + " times in rapid succession");

			for (int i = 0; i < getConfig().getBurstCount(); i++) {
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException("Sampling thread interrupted");
				}

				try {
					samplerTask.run(i, payloads.get(i % payloads.size()));
				} catch (Exception e) {
					logger.error("Failed to run the sample task for sample #" + i, e);
				}
			}
		}

		protected void forEach(SamplerTask<byte[]> samplerTask, Iterator<byte[]> iterator) throws InterruptedException {
			logger.info(getClass().getName() + " will run the task " + getConfig().getBurstCount() + " times in rapid succession");

			for (int i = 0; i < getConfig().getBurstCount(); i++) {
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException("Sampling thread interrupted");
				}

				try {
					samplerTask.run(i, iterator.next());
				} catch (Exception e) {
					logger.error("Failed to run the sample task for sample #" + i, e);
				}
			}
		}

	}

}