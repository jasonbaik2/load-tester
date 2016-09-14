package me.jasonbaik.loadtester.valueobject;

import java.io.Serializable;
import java.util.Map;

public class Broker implements Serializable {

	private static final long serialVersionUID = 1L;

	private String hostname;
	private String jmxUrl;
	private String username;
	private String password;

	private String keyStore;
	private String keyStorePassword;
	private String trustStore;
	private String trustStorePassword;

	private Map<String, String> sslProperties;

	private Map<Protocol, Connector> connectors;

	public String getJmxUrl() {
		return jmxUrl;
	}

	public void setJmxUrl(String jmxUrl) {
		this.jmxUrl = jmxUrl;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Map<Protocol, Connector> getConnectors() {
		return connectors;
	}

	public void setConnectors(Map<Protocol, Connector> connectors) {
		this.connectors = connectors;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getKeyStore() {
		return keyStore;
	}

	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getTrustStore() {
		return trustStore;
	}

	public void setTrustStore(String trustStore) {
		this.trustStore = trustStore;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public Map<String, String> getSslProperties() {
		return sslProperties;
	}

	public void setSslProperties(Map<String, String> sslProperties) {
		this.sslProperties = sslProperties;
	}

	@Override
	public String toString() {
		return "Broker [hostname=" + hostname + ", jmxUrl=" + jmxUrl + ", username=" + username + ", password=" + password + ", keyStore=" + keyStore + ", keyStorePassword=" + keyStorePassword
				+ ", trustStore=" + trustStore + ", trustStorePassword=" + trustStorePassword + ", sslProperties=" + sslProperties + ", connectors=" + connectors + "]";
	}

}