package me.jasonbaik.loadtester.receiver;

import me.jasonbaik.loadtester.reporter.Reportable;
import me.jasonbaik.loadtester.valueobject.ReportData;

public abstract class Receiver<T extends ReceiverConfig<?>> implements Reportable<ReportData> {

	private T config;

	public Receiver(T config) {
		this.config = config;
	}

	public abstract void init() throws Exception;

	public abstract void destroy() throws Exception;

	public abstract void receive() throws Exception;

	public T getConfig() {
		return config;
	}

	public void setConfig(T config) {
		this.config = config;
	}

}
