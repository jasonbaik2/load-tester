package me.jasonbaik.loadtester.sender;

public abstract class AbstractSender<T1, T2 extends AbstractSenderConfig<?>> implements Sender<T1> {

	private T2 config;
	private volatile String state = "Connecting";

	public AbstractSender(T2 config) {
		this.config = config;
	}

	protected void interruptIfInterrupted(String msg) throws InterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			try {
				interrupt();
			} catch (Exception e) {
				throw new InterruptedException(msg + ", " + e.getMessage());
			}
		}
	}

	public void interrupt() throws Exception {
		// No op
	}

	public T2 getConfig() {
		return config;
	}

	public void setConfig(T2 config) {
		this.config = config;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

}
