package me.jasonbaik.loadtester.sampler.impl;

import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sampler.SamplerConfig;

class RandomSamplerConfig extends SamplerConfig<RandomSampler> {

	private static final long serialVersionUID = 1L;

	private int expectedInterval;
	private TimeUnit expectedIntervalUnit;

	@Override
	public Class<RandomSampler> getSamplerClass() {
		return RandomSampler.class;
	}

	public int getExpectedInterval() {
		return this.expectedInterval;
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

	@Override
	public String toString() {
		return "RandomSamplerConfig [expectedInterval=" + expectedInterval + ", expectedIntervalUnit=" + expectedIntervalUnit + "]";
	}

}