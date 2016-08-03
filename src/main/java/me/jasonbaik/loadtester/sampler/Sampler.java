package me.jasonbaik.loadtester.sampler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.reporter.Reportable;
import me.jasonbaik.loadtester.valueobject.ReportData;

public abstract class Sampler<T1, T2 extends SamplerConfig<?>> implements Reportable<ReportData> {

	private T2 config;

	public Sampler(T2 config) {
		this.config = config;
	}

	public abstract void destroy();

	public abstract void forEach(SamplerTask<T1> samplerTask, List<byte[]> payloads) throws InterruptedException;

	public abstract void forEach(SamplerTask<T1> samplerTask, PayloadIterator<byte[]> payloadIterator) throws InterruptedException;

	public abstract void during(SamplerTask<T1> samplerTask, List<byte[]> payloads, long duration, TimeUnit unit) throws InterruptedException;

	public abstract void during(SamplerTask<T1> samplerTask, PayloadIterator<byte[]> payloadIterator, long duration, TimeUnit unit) throws InterruptedException;

	@Override
	public ArrayList<ReportData> report() {
		throw new UnsupportedOperationException();
	}

	public T2 getConfig() {
		return config;
	}

	public void setConfig(T2 config) {
		this.config = config;
	}

}
