package me.jasonbaik.loadtester.receiver;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import me.jasonbaik.loadtester.reporter.Loggable;
import me.jasonbaik.loadtester.reporter.Reportable;
import me.jasonbaik.loadtester.valueobject.ReportData;

public interface Receiver extends Reportable<ReportData>, Loggable {

	@PostConstruct
	public abstract void init() throws Exception;

	@PreDestroy
	public abstract void destroy() throws Exception;

	public abstract void interrupt() throws Exception;

	public abstract void receive() throws Exception;

}
