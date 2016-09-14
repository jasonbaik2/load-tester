package me.jasonbaik.loadtester.client;

public interface MQTTClientCallback {

	public void onSuccess() throws Exception;

	public void onFailure() throws Exception;

}
