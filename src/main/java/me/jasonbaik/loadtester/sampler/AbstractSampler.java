package me.jasonbaik.loadtester.sampler;

import java.util.ArrayList;

import me.jasonbaik.loadtester.reporter.Reportable;
import me.jasonbaik.loadtester.valueobject.ReportData;

public abstract class AbstractSampler<T1, T2 extends AbstractSamplerConfig<?>> implements Sampler<T1>, Reportable<ReportData> {

	private T2 config;

	public abstract void destroy();

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
