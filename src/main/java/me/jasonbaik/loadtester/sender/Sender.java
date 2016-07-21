package me.jasonbaik.loadtester.sender;

import me.jasonbaik.loadtester.reporter.Reportable;
import me.jasonbaik.loadtester.sampler.Sampler;
import me.jasonbaik.loadtester.valueobject.ReportData;

public abstract class Sender<T1, T2 extends SenderConfig<?>> implements Reportable<ReportData> {

	private T2 config;

	public Sender(T2 config) {
		this.config = config;
	}

	public abstract void init() throws Exception;

	public abstract void destroy() throws Exception;

	public abstract void send(Sampler<T1, ?> sampler) throws Exception;

	public T2 getConfig() {
		return config;
	}

	public void setConfig(T2 config) {
		this.config = config;
	}

}
