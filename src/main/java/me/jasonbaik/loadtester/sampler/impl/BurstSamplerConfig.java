package me.jasonbaik.loadtester.sampler.impl;

import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sampler.SamplerConfig;

class BurstSamplerConfig extends SamplerConfig<BurstSampler> {

	private static final long serialVersionUID = 1L;

	private long burstInterval;
	private TimeUnit burstIntervalUnit;
	private int burstCount;

	@Override
	public Class<BurstSampler> getSamplerClass() {
		return BurstSampler.class;
	}

	public long getBurstInterval() {
		return burstInterval;
	}

	public void setBurstInterval(long burstInterval) {
		this.burstInterval = burstInterval;
	}

	public int getBurstCount() {
		return burstCount;
	}

	public void setBurstCount(int burstCount) {
		this.burstCount = burstCount;
	}

	public TimeUnit getBurstIntervalUnit() {
		return burstIntervalUnit;
	}

	public void setBurstIntervalUnit(TimeUnit burstIntervalUnit) {
		this.burstIntervalUnit = burstIntervalUnit;
	}

	@Override
	public String toString() {
		return "BurstSamplerConfig [burstInterval=" + burstInterval + ", burstIntervalUnit=" + burstIntervalUnit + ", burstCount=" + burstCount + "]";
	}

}
