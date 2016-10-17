package me.jasonbaik.loadtester.client;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.ExtendedListener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

public class FuseSourceMQTTClientAdapter implements MQTTClientWrapper {

	private MQTT client;
	private CallbackConnection callbackConnection;

	public FuseSourceMQTTClientAdapter(MQTT client) {
		this.client = client;
		this.callbackConnection = client.callbackConnection();
	}

	@Override
	public void connect(final MQTTClientCallback cb) {
		callbackConnection.connect(new Callback<Void>() {

			@Override
			public void onSuccess(Void value) {
				try {
					cb.onSuccess();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(Throwable value) {
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
	public void disconnect() {
		callbackConnection.disconnect(null);
	}

	@Override
	public void onMessage(final Runnable messageCb) {
		callbackConnection.listener(new ExtendedListener() {

			@Override
			public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
				ack.run();
				messageCb.run();
			}

			@Override
			public void onFailure(Throwable value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onDisconnected() {
				// TODO Auto-generated method stub

			}

			@Override
			public void onConnected() {
				// TODO Auto-generated method stub

			}

			@Override
			public void onPublish(UTF8Buffer topic, Buffer body, Callback<Callback<Void>> ack) {
				ack.onSuccess(null);
				messageCb.run();
			}
		});
	}

	@Override
	public void subscribe(String connectionId, int qos, final MQTTClientCallback cb) throws Exception {
		callbackConnection.subscribe(new Topic[] { new Topic(connectionId, convertQoS(qos)) }, new Callback<byte[]>() {

			@Override
			public void onSuccess(byte[] value) {
				try {
					cb.onSuccess();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(Throwable value) {
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
	public void publishAsync(String topic, byte[] payload, int qos, boolean retain, final MQTTClientCallback cb) throws Exception {
		callbackConnection.publish(topic, payload, convertQoS(qos), retain, new Callback<Void>() {

			@Override
			public void onSuccess(Void value) {
				try {
					cb.onSuccess();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(Throwable value) {
				try {
					cb.onFailure();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});
	}

	private QoS convertQoS(int qos) {
		switch (qos) {
		case 0:
			return QoS.AT_MOST_ONCE;
		case 1:
			return QoS.AT_LEAST_ONCE;
		case 2:
			return QoS.EXACTLY_ONCE;
		default:
			throw new IllegalArgumentException("Unknown QoS");
		}
	}

	public MQTT getClient() {
		return client;
	}

	public void setClient(MQTT client) {
		this.client = client;
	}

	@Override
	public String getConnectionId() {
		return client.getClientId().toString();
	}

}