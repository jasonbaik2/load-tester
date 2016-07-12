package me.jasonbaik.loadtester.valueobject;

import java.io.Serializable;
import java.util.List;

import me.jasonbaik.loadtester.sampler.Sampler;
import me.jasonbaik.loadtester.sampler.SamplerConfig;
import me.jasonbaik.loadtester.sender.Sender;
import me.jasonbaik.loadtester.sender.SenderConfig;

public class Send<T1, T2> implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	private SamplerConfig<Sampler<T1, ?>> samplerConfig;
	private SenderConfig<Sender<T1, ?>> senderConfig;
	private String clientUUID;
	private List<ReportData> reportData;

	public SamplerConfig<Sampler<T1, ?>> getSamplerConfig() {
		return samplerConfig;
	}

	public void setSamplerConfig(SamplerConfig<Sampler<T1, ?>> samplerConfig) {
		this.samplerConfig = samplerConfig;
	}

	public SenderConfig<Sender<T1, ?>> getSenderConfig() {
		return senderConfig;
	}

	public void setSenderConfig(SenderConfig<Sender<T1, ?>> senderConfig) {
		this.senderConfig = senderConfig;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ReportData> getReportData() {
		return reportData;
	}

	public void setReportData(List<ReportData> reportData) {
		this.reportData = reportData;
	}

	public String getClientUUID() {
		return clientUUID;
	}

	public void setClientUUID(String clientUUID) {
		this.clientUUID = clientUUID;
	}

	@Override
	public String toString() {
		return "Send [name=" + name + ", samplerConfig=" + samplerConfig + ", senderConfig=" + senderConfig + ", clientUUID=" + clientUUID + "]";
	}

}
