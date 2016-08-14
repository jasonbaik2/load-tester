package me.jasonbaik.loadtester.reporter.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import me.jasonbaik.loadtester.reporter.Reportable;
import me.jasonbaik.loadtester.valueobject.ReportData;

public class ConnectionStatReporter implements Reportable<ReportData> {

	private AtomicInteger numConnectionsInitiated = new AtomicInteger();
	private AtomicInteger numConnectionsEstablished = new AtomicInteger();
	private AtomicInteger numSubscriptionsInitiated = new AtomicInteger();
	private AtomicInteger numSubscriptionsEstablished = new AtomicInteger();

	private Map<String, Long> initTimes = Collections.synchronizedMap(new HashMap<String, Long>());
	private Map<String, Long> compTimes = Collections.synchronizedMap(new HashMap<String, Long>());
	private Map<String, Long> subCompTimes = Collections.synchronizedMap(new HashMap<String, Long>());

	public void recordConnectionInit(String connectionId) {
		numConnectionsInitiated.incrementAndGet();
		initTimes.put(connectionId, System.currentTimeMillis());
	}

	public void recordConnectionComp(String connectionId) {
		numConnectionsEstablished.incrementAndGet();
		compTimes.put(connectionId, System.currentTimeMillis());
	}

	public void recordSubscriptionInit(String connectionId) {
		numSubscriptionsInitiated.incrementAndGet();
	}

	public void recordSubscriptionComp(String connectionId) {
		numSubscriptionsEstablished.incrementAndGet();
		subCompTimes.put(connectionId, System.currentTimeMillis());
	}

	@Override
	public ArrayList<ReportData> report() {
		StringBuilder sb = new StringBuilder("Connection ID,conn_init,conn_comp,sub_comp\n");
		ArrayList<ReportData> data = new ArrayList<ReportData>(1);

		synchronized (initTimes) {
			synchronized (compTimes) {
				synchronized (subCompTimes) {
					for (Iterator<Entry<String, Long>> iter = initTimes.entrySet().iterator(); iter.hasNext();) {
						Entry<String, Long> e = iter.next();
						sb.append(e.getKey()).append(",").append(e.getValue()).append(",");
						sb.append(compTimes.get(e.getKey()) == null ? "-1" : compTimes.get(e.getKey())).append(",");
						sb.append(subCompTimes.get(e.getKey()) == null ? "-1" : subCompTimes.get(e.getKey()));
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

	public AtomicInteger getNumConnectionsInitiated() {
		return numConnectionsInitiated;
	}

	public void setNumConnectionsInitiated(AtomicInteger numConnectionsInitiated) {
		this.numConnectionsInitiated = numConnectionsInitiated;
	}

	public AtomicInteger getNumConnectionsEstablished() {
		return numConnectionsEstablished;
	}

	public void setNumConnectionsEstablished(AtomicInteger numConnectionsEstablished) {
		this.numConnectionsEstablished = numConnectionsEstablished;
	}

	public AtomicInteger getNumSubscriptionsInitiated() {
		return numSubscriptionsInitiated;
	}

	public void setNumSubscriptionsInitiated(AtomicInteger numSubscriptionsInitiated) {
		this.numSubscriptionsInitiated = numSubscriptionsInitiated;
	}

	public AtomicInteger getNumSubscriptionsEstablished() {
		return numSubscriptionsEstablished;
	}

	public void setNumSubscriptionsEstablished(AtomicInteger numSubscriptionsEstablished) {
		this.numSubscriptionsEstablished = numSubscriptionsEstablished;
	}

}