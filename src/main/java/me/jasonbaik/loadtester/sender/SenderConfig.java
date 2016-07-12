package me.jasonbaik.loadtester.sender;

import java.io.Serializable;

public abstract class SenderConfig<T extends Sender<?, ?>> implements Serializable {

	private static final long serialVersionUID = 1L;

	public abstract Class<T> getSenderClass();

	public String describe() {
		return toString();
	}

}
