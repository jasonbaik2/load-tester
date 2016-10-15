package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;
import java.util.List;

import me.jasonbaik.loadtester.sampler.Sampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;

public class PoissonSampler<T> implements Sampler<T> {

	@Override
	public void sample(SamplerTask<T> sampleTask, List<T> payloads) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sample(SamplerTask<T> sampleTask, Iterator<T> payloadGenerator) throws InterruptedException {
		// TODO Auto-generated method stub

	}

}
