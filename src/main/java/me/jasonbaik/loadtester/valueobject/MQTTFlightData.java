package me.jasonbaik.loadtester.valueobject;

public class MQTTFlightData {

	private String messageId;

	private long pubTimeMillis = -1;
	private long pubTime = -1;
	private long pubAckReceiveTime = -1;
	private long pubRecReceiveTime = -1;
	private long pubRelSendTime = -1;
	private long pubCompReceiveTime = -1;
	private long replyTime = -1;

	public void setPubTimeMillis(long pubTimeMillis) {
		this.pubTimeMillis = pubTimeMillis;
	}

	public long getPubTimeMillis() {
		return pubTimeMillis;
	}

	public long getPubTime() {
		return pubTime;
	}

	public void setPubTime(long publishTime) {
		this.pubTime = publishTime;
	}

	public long getPubAckReceiveTime() {
		return pubAckReceiveTime;
	}

	public void setPubAckReceiveTime(long pubAckReceiveTime) {
		this.pubAckReceiveTime = pubAckReceiveTime;
	}

	public long getPubRecReceiveTime() {
		return pubRecReceiveTime;
	}

	public void setPubRecReceiveTime(long pubRecReceiveTime) {
		this.pubRecReceiveTime = pubRecReceiveTime;
	}

	public long getPubRelSendTime() {
		return pubRelSendTime;
	}

	public void setPubRelSendTime(long pubRelSendTime) {
		this.pubRelSendTime = pubRelSendTime;
	}

	public long getPubCompReceiveTime() {
		return pubCompReceiveTime;
	}

	public void setPubCompReceiveTime(long pubCompReceiveTime) {
		this.pubCompReceiveTime = pubCompReceiveTime;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public long getReplyTime() {
		return replyTime;
	}

	public void setReplyTime(long replyTime) {
		this.replyTime = replyTime;
	}

}
