package me.jasonbaik.loadtester.sampler.impl;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sampler.AbstractFixedDurationSamplerConfig;

public class FixedDurationBurstSamplerConfig extends AbstractFixedDurationSamplerConfig<FixedDurationBurstSampler> implements Serializable {

	private static final long serialVersionUID = 1L;

	private long burstCount;
	private long burstInterval;
	private TimeUnit burstIntervalUnit;

	public Class<FixedDurationBurstSampler> getSamplerClass() {
		return FixedDurationBurstSampler.class;
	}

	public long getBurstCount() {
		return burstCount;
	}

	public void setBurstCount(long burstCount) {
		this.burstCount = burstCount;
	}

	public long getBurstInterval() {
		return burstInterval;
	}

	public void setBurstInterval(long burstInterval) {
		this.burstInterval = burstInterval;
	}

	public TimeUnit getBurstIntervalUnit() {
		return burstIntervalUnit;
	}

	public void setBurstIntervalUnit(TimeUnit burstIntervalUnit) {
		this.burstIntervalUnit = burstIntervalUnit;
	}

}