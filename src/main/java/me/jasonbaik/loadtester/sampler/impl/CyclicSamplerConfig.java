package me.jasonbaik.loadtester.sampler.impl;

import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sampler.SamplerConfig;

class CyclicSamplerConfig extends SamplerConfig<CyclicSampler> {

	private static final long serialVersionUID = 1L;

	private long interval;
	private TimeUnit intervalUnit;

	@Override
	public Class<CyclicSampler> getSamplerClass() {
		return CyclicSampler.class;
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public TimeUnit getIntervalUnit() {
		return intervalUnit;
	}

	public void setIntervalUnit(TimeUnit intervalUnit) {
		this.intervalUnit = intervalUnit;
	}

	@Override
	public String toString() {
		return "CyclicSamplerConfig [interval=" + interval + ", intervalUnit=" + intervalUnit + "]";
	}

}
