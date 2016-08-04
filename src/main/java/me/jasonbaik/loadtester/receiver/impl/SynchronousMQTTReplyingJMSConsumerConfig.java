package me.jasonbaik.loadtester.receiver.impl;


public class SynchronousMQTTReplyingJMSConsumerConfig extends AbstractMQTTReplyingJMSConsumerConfig<SynchronousMQTTReplyingJMSConsumer> {

	private static final long serialVersionUID = 1L;

	@Override
	public Class<SynchronousMQTTReplyingJMSConsumer> getReceiverClass() {
		return SynchronousMQTTReplyingJMSConsumer.class;
	}

}