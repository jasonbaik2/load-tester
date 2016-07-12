package me.jasonbaik.loadtester.valueobject;

import java.io.InputStream;
import java.util.Scanner;

import org.fusesource.hawtbuf.ByteArrayInputStream;

public class Payload {

	private String connectionId;
	private int messageId;
	private byte[] data;

	public Payload(String connectionId, int messageId, byte[] data) {
		super();
		this.connectionId = connectionId;
		this.messageId = messageId;
		this.data = data;
	}

	public String getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}

	public int getMessageId() {
		return messageId;
	}

	public void setMessageId(int messageId) {
		this.messageId = messageId;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public static byte[] toBytes(String connectionId, int messageId, byte[] data) {
		byte[] idPair = (connectionId + "\n" + messageId + "\n").getBytes();
		byte[] payload = new byte[idPair.length + data.length];
		System.arraycopy(idPair, 0, payload, 0, idPair.length);
		System.arraycopy(data, 0, payload, idPair.length, data.length);
		return payload;
	}

	public static String[] extractIdPair(byte[] bytes) {
		InputStream is = null;
		Scanner sc = null;
		is = new ByteArrayInputStream(bytes);
		sc = new Scanner(is);

		String[] idPair = new String[2];

		try {
			idPair[0] = sc.nextLine();
			idPair[1] = sc.nextLine();
			return idPair;

		} finally {
			sc.close();
		}
	}

	public static String extractConnectionId(byte[] bytes) {
		return extractIdPair(bytes)[0];
	}

	public static String extractMessageId(byte[] bytes) {
		return extractIdPair(bytes)[1];

	}

	public static String extractUniqueId(byte[] bytes) {
		String[] idPair = extractIdPair(bytes);
		return idPair[0] + "-" + idPair[1];
	}

}
