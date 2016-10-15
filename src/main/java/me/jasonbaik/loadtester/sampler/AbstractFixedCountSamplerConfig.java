package me.jasonbaik.loadtester.sampler;

import java.io.Serializable;

public abstract class AbstractFixedCountSamplerConfig<T extends AbstractFixedCountSampler<?, ?>> extends AbstractSamplerConfig<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	private long count;

	public abstract Class<T> getSamplerClass();

	public String describe() {
		return toString();
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

}