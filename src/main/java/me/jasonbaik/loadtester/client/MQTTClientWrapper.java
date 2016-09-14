package me.jasonbaik.loadtester.client;

public interface MQTTClientWrapper {

	public void connect(MQTTClientCallback connectCb) throws Exception;

	public void disconnect() throws Exception;

	public void subscribe(String connectionId, int qos, MQTTClientCallback cb) throws Exception;

	public void publishAsync(String topic, byte[] payload, int qos, boolean retain, MQTTClientCallback cb) throws Exception;

	public void onMessage(Runnable messageCb);

	public String getConnectionId();

}
