package me.jasonbaik.loadtester.sampler;

public interface SamplerTask<T> {

	public void run(int index, T payload) throws Exception;

}
