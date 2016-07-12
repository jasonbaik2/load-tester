package me.jasonbaik.loadtester.sampler.impl;

import me.jasonbaik.loadtester.sampler.SamplerConfig;

class PoissonSamplerConfig extends SamplerConfig<PoissonSampler> {

	private static final long serialVersionUID = 1L;

	@Override
	public Class<PoissonSampler> getSamplerClass() {
		return PoissonSampler.class;
	}

}
