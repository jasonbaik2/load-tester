package me.jasonbaik.loadtester.sender;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import me.jasonbaik.loadtester.reporter.Loggable;
import me.jasonbaik.loadtester.reporter.Reportable;
import me.jasonbaik.loadtester.valueobject.ReportData;

public interface Sender<T> extends Reportable<ReportData>, Loggable {

	@PostConstruct
	public void init() throws Exception;

	@PreDestroy
	public void destroy() throws Exception;

	public void interrupt() throws Exception;

	public void send() throws Exception;

}
