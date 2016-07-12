package me.jasonbaik.loadtester.sampler.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sampler.PayloadIterator;
import me.jasonbaik.loadtester.sampler.Sampler;
import me.jasonbaik.loadtester.sampler.SamplerTask;

public class PoissonSampler extends Sampler<byte[], PoissonSamplerConfig> {

	public PoissonSampler(PoissonSamplerConfig config) {
		super(config);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void forEach(SamplerTask<byte[]> samplerTask, List<byte[]> payloads) {
		// TODO Auto-generated method stub

	}

	@Override
	public void forEach(SamplerTask<byte[]> samplerTask, PayloadIterator<byte[]> payloadGenerator) {
		// TODO Auto-generated method stub

	}

	@Override
	public void during(SamplerTask<byte[]> sampleTask, List<byte[]> payloads, long seconds, TimeUnit unit) {
		// TODO Auto-generated method stub

	}

	@Override
	public void during(SamplerTask<byte[]> sampleTask, PayloadIterator<byte[]> payloadGenerator, long seconds, TimeUnit unit) {
		// TODO Auto-generated method stub

	}

}
