package me.jasonbaik.loadtester.receiver.impl;

import me.jasonbaik.loadtester.receiver.AbstractMQTTReplyingJMSConsumerConfig;

public class SynchronousMQTTReplyingJMSConsumerConfig extends AbstractMQTTReplyingJMSConsumerConfig<SynchronousMQTTReplyingJMSConsumer> {

	private static final long serialVersionUID = 1L;

	@Override
	public Class<SynchronousMQTTReplyingJMSConsumer> getReceiverClass() {
		return SynchronousMQTTReplyingJMSConsumer.class;
	}

}