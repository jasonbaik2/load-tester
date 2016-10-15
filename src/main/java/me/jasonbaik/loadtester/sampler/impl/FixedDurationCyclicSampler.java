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

public class FixedDurationCyclicSampler extends AbstractFixedDurationSampler<byte[], FixedDurationCyclicSamplerConfig> {

	private static final Logger logger = LogManager.getLogger(FixedDurationCyclicSampler.class);

	private ScheduledExecutorService es;

	@Override
	public void destroy() {
		if (this.es != null) {
			this.es.shutdownNow();
		}
	}

	@Override
	public void during(SamplerTask<byte[]> samplerTask, List<byte[]> payloads) throws InterruptedException {
		executeCycles(new FixedDurationRunnable(samplerTask, payloads, System.nanoTime() + TimeUnit.NANOSECONDS.convert(getConfig().getDuration(), getConfig().getUnit())));
	}

	@Override
	public void during(SamplerTask<byte[]> samplerTask, Iterator<byte[]> Iterator) throws InterruptedException {
		executeCycles(new FixedDurationRunnable(samplerTask, Iterator, System.nanoTime() + TimeUnit.NANOSECONDS.convert(getConfig().getDuration(), getConfig().getUnit())));
	}

	private void executeCycles(FixedDurationRunnable runnable) throws InterruptedException {
		logger.info("CyclicSampler will run the task for " + getConfig().getDuration() + " " + getConfig().getUnit() + " with a " + getConfig().getInterval() + " " + getConfig().getIntervalUnit()
				+ " interval");

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
		private Iterator<byte[]> Iterator;

		private FixedDurationRunnable(SamplerTask<byte[]> task, List<byte[]> payloads, long endTime) {
			this.task = task;
			this.payloads = payloads;
			this.endTime = endTime;
		}

		private FixedDurationRunnable(SamplerTask<byte[]> task, Iterator<byte[]> Iterator, long endTime) {
			this.task = task;
			this.Iterator = Iterator;
			this.endTime = endTime;
		}

		@Override
		public void run() {
			if (System.nanoTime() >= endTime) {
				logger.info("Sampler has reached the end of the duration. Shutting down the sampler...");
				es.shutdown();
				return;
			}

			if (Iterator != null) {
				try {
					task.run(index, Iterator.next());
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
