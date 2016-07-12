package me.jasonbaik.loadtester.valueobject;

import java.io.Serializable;
import java.util.List;

import me.jasonbaik.loadtester.receiver.Receiver;
import me.jasonbaik.loadtester.receiver.ReceiverConfig;

import org.springframework.beans.factory.annotation.Required;

public class Receive<T1> implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	private ReceiverConfig<Receiver<?>> receiverConfig;
	private String clientUUID;
	private List<ReportData> reportData;

	public ReceiverConfig<Receiver<?>> getReceiverConfig() {
		return receiverConfig;
	}

	public void setReceiverConfig(ReceiverConfig<Receiver<?>> receiverConfig) {
		this.receiverConfig = receiverConfig;
	}

	public String getName() {
		return name;
	}

	@Required
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
		return "Receive [name=" + name + ", receiverConfig=" + receiverConfig + ", clientUUID=" + clientUUID + "]";
	}

}
