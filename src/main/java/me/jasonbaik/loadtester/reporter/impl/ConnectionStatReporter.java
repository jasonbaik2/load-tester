package me.jasonbaik.loadtester.reporter.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import me.jasonbaik.loadtester.reporter.Reportable;
import me.jasonbaik.loadtester.valueobject.ReportData;

public class ConnectionStatReporter implements Reportable<ReportData> {

	private Map<String, Long> initTimes = Collections.synchronizedMap(new HashMap<String, Long>());
	private Map<String, Long> compTimes = Collections.synchronizedMap(new HashMap<String, Long>());
	private Map<String, Long> subCompTimes = Collections.synchronizedMap(new HashMap<String, Long>());

	public void recordConnectionInit(String connectionId) {
		initTimes.put(connectionId, System.currentTimeMillis());
	}

	public void recordConnectionComp(String connectionId) {
		compTimes.put(connectionId, System.currentTimeMillis());
	}

	public void recordSubscriptionComp(String connectionId) {
		subCompTimes.put(connectionId, System.currentTimeMillis());
	}

	@Override
	public ArrayList<ReportData> report() throws InterruptedException {
		StringBuilder sb = new StringBuilder("Connection ID,conn_init,conn_comp,sub_comp\n");
		ArrayList<ReportData> data = new ArrayList<ReportData>(1);

		synchronized (initTimes) {
			synchronized (compTimes) {
				synchronized (subCompTimes) {
					for (Iterator<Entry<String, Long>> iter = initTimes.entrySet().iterator(); iter.hasNext();) {
						Entry<String, Long> e = iter.next();
						sb.append(e.getKey()).append(",").append(e.getValue()).append(",");
						sb.append(compTimes.get(e.getKey()) == null ? "" : compTimes.get(e.getKey())).append(",");
						sb.append(subCompTimes.get(e.getKey()) == null ? "" : subCompTimes.get(e.getKey()));
						sb.append("\n");
					}
				}
			}
		}

		data.add(new ReportData("Connection_Stats.csv", sb.toString().getBytes()));
		return data;
	}

	@Override
	public void destroy() {
		synchronized (initTimes) {
			initTimes.clear();
		}
		synchronized (compTimes) {
			compTimes.clear();
		}
		synchronized (subCompTimes) {
			subCompTimes.clear();
		}
	}

}