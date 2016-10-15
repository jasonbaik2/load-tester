package me.jasonbaik.loadtester.sender;

public class SenderFactory {

	public static <T extends Sender<?>> T newInstance(AbstractSenderConfig<T> config) throws Exception {
		T sender = config.getSenderClass().cast(config.getSenderClass().getConstructor(config.getClass()).newInstance(config));
		sender.init();
		return sender;
	}

}
