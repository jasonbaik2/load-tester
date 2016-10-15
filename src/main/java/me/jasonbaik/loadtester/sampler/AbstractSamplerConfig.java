package me.jasonbaik.loadtester.sampler;

import java.io.Serializable;

public abstract class AbstractSamplerConfig<T extends Sampler<?>> implements Serializable {

	private static final long serialVersionUID = 1L;

	public abstract Class<T> getSamplerClass();

	public String describe() {
		return toString();
	}

}