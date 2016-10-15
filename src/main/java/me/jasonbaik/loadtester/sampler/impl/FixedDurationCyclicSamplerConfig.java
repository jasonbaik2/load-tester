package me.jasonbaik.loadtester.sampler.impl;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sampler.AbstractFixedDurationSamplerConfig;

public abstract class FixedDurationCyclicSamplerConfig extends AbstractFixedDurationSamplerConfig<FixedDurationCyclicSampler> implements Serializable {

	private static final long serialVersionUID = 1L;

	private long interval;
	private TimeUnit intervalUnit;

	public Class<FixedDurationCyclicSampler> getSamplerClass() {
		return FixedDurationCyclicSampler.class;
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

}