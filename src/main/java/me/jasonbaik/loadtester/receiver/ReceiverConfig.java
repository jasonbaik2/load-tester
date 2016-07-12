package me.jasonbaik.loadtester.receiver;

import java.io.Serializable;

public abstract class ReceiverConfig<T extends Receiver<?>> implements Serializable {

	private static final long serialVersionUID = 1L;

	public abstract Class<T> getReceiverClass();

	public String describe() {
		return toString();
	}

}
