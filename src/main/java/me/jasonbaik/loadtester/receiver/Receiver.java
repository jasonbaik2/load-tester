package me.jasonbaik.loadtester.receiver;

import me.jasonbaik.loadtester.reporter.Loggable;
import me.jasonbaik.loadtester.reporter.Reportable;
import me.jasonbaik.loadtester.valueobject.ReportData;

public interface Receiver extends Reportable<ReportData>, Loggable {

	public abstract void init() throws Exception;

	public abstract void destroy() throws Exception;

	public abstract void interrupt() throws Exception;

	public abstract void receive() throws Exception;

}
