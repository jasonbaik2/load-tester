package me.jasonbaik.loadtester.sender;

import me.jasonbaik.loadtester.reporter.Loggable;
import me.jasonbaik.loadtester.reporter.Reportable;
import me.jasonbaik.loadtester.sampler.Sampler;
import me.jasonbaik.loadtester.valueobject.ReportData;

public interface Sender<T1> extends Reportable<ReportData>, Loggable {

	public void init() throws Exception;

	public void destroy() throws Exception;

	public void interrupt() throws Exception;

	public void send(Sampler<T1, ?> sampler) throws Exception;

}
