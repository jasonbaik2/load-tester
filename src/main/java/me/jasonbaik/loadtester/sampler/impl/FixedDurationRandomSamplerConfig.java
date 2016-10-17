package me.jasonbaik.loadtester.sampler.impl;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sampler.AbstractFixedDurationSamplerConfig;

public class FixedDurationRandomSamplerConfig extends AbstractFixedDurationSamplerConfig<FixedDurationRandomSampler> implements Serializable {

	private static final long serialVersionUID = 1L;

	private int expectedInterval;
	private TimeUnit expectedIntervalUnit;

	public Class<FixedDurationRandomSampler> getSamplerClass() {
		return FixedDurationRandomSampler.class;
	}

	public int getExpectedInterval() {
		return expectedInterval;
	}

	public void setExpectedInterval(int expectedInterval) {
		this.expectedInterval = expectedInterval;
	}

	public TimeUnit getExpectedIntervalUnit() {
		return expectedIntervalUnit;
	}

	public void setExpectedIntervalUnit(TimeUnit expectedIntervalUnit) {
		this.expectedIntervalUnit = expectedIntervalUnit;
	}

}