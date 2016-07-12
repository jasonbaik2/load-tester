package me.jasonbaik.loadtester.valueobject;

import java.io.Serializable;

public class ReportData implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	private byte[] data;

	public ReportData(String name, byte[] data) {
		this.name = name;
		this.data = data;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "ReportData [name=" + name + ", data=" + new String(data) + "]";
	}

}
