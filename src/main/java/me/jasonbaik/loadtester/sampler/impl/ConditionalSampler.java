package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import me.jasonbaik.loadtester.sampler.Sampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;

public class ConditionalSampler<T> implements Sampler<T> {

	private static final Logger logger = LogManager.getLogger(ConditionalSampler.class);

	private Supplier<Boolean> condition;

	public ConditionalSampler(Supplier<Boolean> condition) {
		this.condition = condition;
	}

	@Override
	public void sample(SamplerTask<T> sampleTask, List<T> payloads) throws InterruptedException {
		for (int index = 0; condition.get(); index++) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}

			try {
				sampleTask.run(index, payloads.get(index % payloads.size()));
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	@Override
	public void sample(SamplerTask<T> sampleTask, Iterator<T> payloadGenerator) throws InterruptedException {
		for (int index = 0; condition.get() && payloadGenerator.hasNext(); index++) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}

			try {
				sampleTask.run(index, payloadGenerator.next());
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

}
