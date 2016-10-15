package me.jasonbaik.loadtester.receiver;

public abstract class AbstractReceiver<T extends AbstractReceiverConfig<?>> implements Receiver {

	private T config;
	private volatile String state;

	public AbstractReceiver(T config) {
		this.config = config;
	}

	@Override
	public void interrupt() throws Exception {
		// No op
	}

	public T getConfig() {
		return config;
	}

	public void setConfig(T config) {
		this.config = config;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

}
