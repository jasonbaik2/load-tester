package me.jasonbaik.loadtester.receiver;

public class ReceiverFactory {

	public static <T extends Receiver> T newInstance(AbstractReceiverConfig<T> config) throws Exception {
		T receiver = config.getReceiverClass().cast(config.getReceiverClass().getConstructor(config.getClass()).newInstance(config));
		receiver.init();
		return receiver;
	}

}
