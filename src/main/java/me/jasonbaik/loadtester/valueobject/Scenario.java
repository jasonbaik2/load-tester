package me.jasonbaik.loadtester.valueobject;

import java.io.Serializable;
import java.util.List;

public class Scenario<S1, S2, R1> implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	private List<Send<S1, S2>> sends;
	private List<Receive<R1>> receives;
	private long maxAttackTimeSeconds;
	private String reportDir;

	public List<Send<S1, S2>> getSends() {
		return sends;
	}

	public void setSends(List<Send<S1, S2>> sends) {
		this.sends = sends;
	}

	public long getMaxAttackTimeSeconds() {
		return maxAttackTimeSeconds;
	}

	public void setMaxAttackTimeSeconds(long maxAttackTimeSeconds) {
		this.maxAttackTimeSeconds = maxAttackTimeSeconds;
	}

	public List<Receive<R1>> getReceives() {
		return receives;
	}

	public void setReceives(List<Receive<R1>> receives) {
		this.receives = receives;
	}

	public String getReportDir() {
		return reportDir;
	}

	public void setReportDir(String reportDir) {
		this.reportDir = reportDir;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
