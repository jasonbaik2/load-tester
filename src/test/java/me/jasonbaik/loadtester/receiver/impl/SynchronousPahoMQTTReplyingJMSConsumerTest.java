package me.jasonbaik.loadtester.receiver.impl;

import javax.inject.Inject;

import me.jasonbaik.loadtester.receiver.impl.SynchronousPahoMQTTReplyingJMSConsumer;
import me.jasonbaik.loadtester.receiver.impl.SynchronousPahoMQTTReplyingJMSConsumerConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:spring/test/context-test-local-paho.xml" })
public class SynchronousPahoMQTTReplyingJMSConsumerTest {

	@Inject
	private SynchronousPahoMQTTReplyingJMSConsumerConfig config;

	@Test
	public void testInit() throws Exception {
		new SynchronousPahoMQTTReplyingJMSConsumer(config).init();
	}

	public SynchronousPahoMQTTReplyingJMSConsumerConfig getConfig() {
		return config;
	}

	public void setConfig(SynchronousPahoMQTTReplyingJMSConsumerConfig config) {
		this.config = config;
	}

}
