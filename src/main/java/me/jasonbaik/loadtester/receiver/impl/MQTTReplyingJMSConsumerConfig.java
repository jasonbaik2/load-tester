package me.jasonbaik.loadtester.receiver.impl;

import me.jasonbaik.loadtester.receiver.AbstractMQTTReplyingJMSConsumerConfig;

public class MQTTReplyingJMSConsumerConfig extends AbstractMQTTReplyingJMSConsumerConfig<MQTTReplyingJMSConsumer> {

	private static final long serialVersionUID = 1L;

	@Override
	public Class<MQTTReplyingJMSConsumer> getReceiverClass() {
		return MQTTReplyingJMSConsumer.class;
	}

}