package me.jasonbaik.loadtester.valueobject;

public class Broker {

	private String url;
	private String jmxUrl;
	private String username;
	private String password;

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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		return "Broker [brokerUrl=" + url + ", jmxUrl=" + jmxUrl + ", username=" + username + "]";
	}

}