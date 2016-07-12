package me.jasonbaik.loadtester;

import java.net.URISyntaxException;

import me.jasonbaik.loadtester.util.MQTTFlightTracer;

import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.junit.Test;

public class MqttClientIT {

	@Test
	public void test() throws URISyntaxException, InterruptedException {
		MQTT mqtt = new MQTT();
		mqtt.setHost("tcp://52.207.232.91:1883");
		mqtt.setClientId("test-client");
		mqtt.setCleanSession(true);
		mqtt.setUserName("admin");
		mqtt.setPassword("admin");
		mqtt.setKeepAlive((short) 0);
		mqtt.setTracer(new MQTTFlightTracer());

		final CallbackConnection conn = mqtt.callbackConnection();
		conn.connect(new Callback<Void>() {

			@Override
			public void onSuccess(Void value) {
				conn.publish("Test Topic", "test message".getBytes(), QoS.EXACTLY_ONCE, false, new Callback<Void>() {

					@Override
					public void onSuccess(Void value) {
						System.out.println("Successfully published");
					}

					@Override
					public void onFailure(Throwable value) {
						value.printStackTrace();
					}

				});
			}

			@Override
			public void onFailure(Throwable value) {
				value.printStackTrace();
			}
		});

		synchronized (this) {
			this.wait();
		}
	}

}
