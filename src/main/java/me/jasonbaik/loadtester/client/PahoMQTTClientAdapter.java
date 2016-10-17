package me.jasonbaik.loadtester.client;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class PahoMQTTClientAdapter implements MQTTClientWrapper {

	private MqttAsyncClient client;
	private MqttConnectOptions options;

	public PahoMQTTClientAdapter(MqttAsyncClient client, MqttConnectOptions options) {
		super();
		this.client = client;
		this.options = options;
	}

	@Override
	public void connect(final MQTTClientCallback cb) throws MqttSecurityException, MqttException {
		client.connect(options, null, new IMqttActionListener() {

			@Override
			public void onSuccess(IMqttToken asyncActionToken) {
				try {
					cb.onSuccess();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
				try {
					cb.onFailure();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});
	}

	@Override
	public void disconnect() throws MqttException {
		client.disconnectForcibly();
	}

	@Override
	public void subscribe(String connectionId, int qos, final MQTTClientCallback cb) throws MqttException {
		client.subscribe(client.getClientId().toString(), qos, null, new IMqttActionListener() {

			@Override
			public void onSuccess(IMqttToken asyncActionToken) {
				try {
					cb.onSuccess();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
				try {
					cb.onFailure();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});
	}

	@Override
	public void publishAsync(String topic, byte[] payload, int qos, boolean retain, final MQTTClientCallback cb) throws MqttPersistenceException, MqttException {
		client.publish(topic, payload, qos, retain, null, new IMqttActionListener() {

			@Override
			public void onSuccess(IMqttToken asyncActionToken) {
				try {
					cb.onSuccess();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
				try {
					cb.onFailure();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});
	}

	@Override
	public void onMessage(final Runnable messageCb) {
		client.setCallback(new MqttCallback() {

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				messageCb.run();
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				// TODO Auto-generated method stub

			}

			@Override
			public void connectionLost(Throwable cause) {
				// TODO Auto-generated method stub

			}
		});
	}

	@Override
	public String getConnectionId() {
		return client.getClientId();
	}

}