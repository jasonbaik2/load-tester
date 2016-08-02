package me.jasonbaik.loadtester.valueobject;

import java.io.Serializable;

public class Connector implements Serializable {

	private static final long serialVersionUID = 1L;

	private String protocol;
	private int port;

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "Connector [protocol=" + protocol + ", port=" + port + "]";
	}

}