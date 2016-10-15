package me.jasonbaik.loadtester.sampler;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public abstract class AbstractFixedDurationSamplerConfig<T extends AbstractFixedDurationSampler<?, ?>> extends AbstractSamplerConfig<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	private long duration;
	private TimeUnit unit;

	public abstract Class<T> getSamplerClass();

	public String describe() {
		return toString();
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public TimeUnit getUnit() {
		return unit;
	}

	public void setUnit(TimeUnit unit) {
		this.unit = unit;
	}

}