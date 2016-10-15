package me.jasonbaik.loadtester.sampler.impl;

import java.io.Serializable;

import me.jasonbaik.loadtester.sampler.AbstractFixedCountSamplerConfig;

public abstract class FixedCountBurstSamplerConfig extends AbstractFixedCountSamplerConfig<FixedCountBurstSampler> implements Serializable {

	private static final long serialVersionUID = 1L;

	public Class<FixedCountBurstSampler> getSamplerClass() {
		return FixedCountBurstSampler.class;
	}

}