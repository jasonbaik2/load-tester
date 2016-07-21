package me.jasonbaik.loadtester.sampler;

public class SamplerFactory {

	public static <T extends Sampler<?, ?>> T newInstance(SamplerConfig<T> config) throws Exception {
		T sampler = config.getSamplerClass().cast(config.getSamplerClass().getConstructor(config.getClass()).newInstance(config));
		return sampler;
	}

}
