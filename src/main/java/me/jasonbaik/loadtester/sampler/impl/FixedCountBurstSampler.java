package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.jasonbaik.loadtester.sampler.AbstractFixedCountSampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;

public class FixedCountBurstSampler extends AbstractFixedCountSampler<byte[], FixedCountBurstSamplerConfig> {

	private static final Logger logger = LogManager.getLogger(FixedCountBurstSampler.class);

	private ScheduledExecutorService es;

	@Override
	public void destroy() {
		if (this.es != null) {
			this.es.shutdownNow();
		}
	}

	@Override
	protected void forEach(SamplerTask<byte[]> samplerTask, List<byte[]> payloads) throws InterruptedException {
		logger.info(getClass().getName() + " will run the task " + getConfig().getCount() + " times in rapid succession");

		for (int i = 0; i < getConfig().getCount(); i++) {
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

	@Override
	protected void forEach(SamplerTask<byte[]> samplerTask, Iterator<byte[]> iterator) throws InterruptedException {
		logger.info(getClass().getName() + " will run the task " + getConfig().getCount() + " times in rapid succession");

		for (int i = 0; i < getConfig().getCount(); i++) {
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