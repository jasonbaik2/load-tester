package me.jasonbaik.loadtester.sampler;

import java.util.Iterator;
import java.util.List;

public interface Sampler<T> {

	public abstract void sample(SamplerTask<T> sampleTask, List<T> payloads) throws InterruptedException;

	public abstract void sample(SamplerTask<T> sampleTask, Iterator<T> payloadGenerator) throws InterruptedException;

}
