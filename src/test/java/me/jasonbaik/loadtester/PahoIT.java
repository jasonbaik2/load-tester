/**
 * 
 */
package me.jasonbaik.loadtester;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * @author hcc5fkv
 *
 */
public class PahoIT {

	private static final int numConn = 1;
	private static final List<MqttAsyncClient> clients = new ArrayList<MqttAsyncClient>(numConn);
	private static final int qos = 0;
	private static final String topic = "Test Topic";
	private static final String payload = "Test Payload";
	private static final String broker = "tcp://localhost:1883";

	public static void main(String[] args) throws InterruptedException {
		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setCleanSession(true);
		connOpts.setUserName("admin");
		connOpts.setPassword("admin".toCharArray());

		for (int i = 0; i < numConn; i++) {
			String clientId = "test_client_" + i;

			try {
				final MqttAsyncClient client = new MqttAsyncClient(broker, clientId, new MemoryPersistence());
				client.connect(connOpts, null, new IMqttActionListener() {

					@Override
					public void onSuccess(IMqttToken asyncActionToken) {
						System.out.println("Connection established with " + asyncActionToken.getClient().getClientId());

						// Publish a message
						// publish(client);

						clients.add(client);
					}

					@Override
					public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
						// TODO Auto-generated method stub

					}
				});

			} catch (MqttException me) {
				System.out.println("reason " + me.getReasonCode());
				System.out.println("msg " + me.getMessage());
				System.out.println("loc " + me.getLocalizedMessage());
				System.out.println("cause " + me.getCause());
				System.out.println("excep " + me);
				me.printStackTrace();
			}
		}

		// rrPublish();
	}

	private static int connNum = 0;

	/**
	 * Round-robin publish
	 */
	public static void rrPublish() {
		ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();
		es.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				publish(clients.get(connNum++));
			}

		}, 0, 1, TimeUnit.SECONDS);
	}

	public static void publish(MqttAsyncClient client) {
		try {
			System.out.println("Publishing message: " + payload);
			MqttMessage message = new MqttMessage(payload.getBytes());
			message.setQos(qos);
			client.publish(topic, message);
			System.out.println("Message published");

		} catch (MqttException me) {
			System.out.println("reason " + me.getReasonCode());
			System.out.println("msg " + me.getMessage());
			System.out.println("loc " + me.getLocalizedMessage());
			System.out.println("cause " + me.getCause());
			System.out.println("excep " + me);
			me.printStackTrace();
		}
	}

}
