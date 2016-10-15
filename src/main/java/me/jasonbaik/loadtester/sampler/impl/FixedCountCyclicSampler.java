package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.jasonbaik.loadtester.sampler.AbstractFixedCountSampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;

public class FixedCountCyclicSampler extends AbstractFixedCountSampler<byte[], FixedCountCyclicSamplerConfig> {

	private static final Logger logger = LogManager.getLogger(FixedCountCyclicSampler.class);

	private ScheduledExecutorService es;

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
	public void forEach(SamplerTask<byte[]> samplerTask, Iterator<byte[]> Iterator) throws InterruptedException {
		executeCycles(new FixedCountRunnable(samplerTask, Iterator));
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
		private Iterator<byte[]> Iterator;

		private FixedCountRunnable(SamplerTask<byte[]> task, List<byte[]> payloads) {
			this.task = task;
			this.payloads = payloads;
		}

		private FixedCountRunnable(SamplerTask<byte[]> task, Iterator<byte[]> Iterator) {
			this.task = task;
			this.Iterator = Iterator;
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

			} else if (Iterator != null) {
				if (!Iterator.hasNext()) {
					logger.info("Sampler has reached the last sample. Shutting down the sampler...");
					es.shutdown();
					return;
				}

				try {
					task.run(index, Iterator.next());
				} catch (Exception e) {
					logger.error("Failed to run the sample task for sample #" + index, e);
				}
			}

			index++;
		}

	}

}
