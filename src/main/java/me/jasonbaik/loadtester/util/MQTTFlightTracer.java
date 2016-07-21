package me.jasonbaik.loadtester.util;

import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import me.jasonbaik.loadtester.valueobject.MQTTFlightData;
import me.jasonbaik.loadtester.valueobject.Payload;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.mqtt.client.Tracer;
import org.fusesource.mqtt.codec.MQTTFrame;
import org.fusesource.mqtt.codec.PUBACK;
import org.fusesource.mqtt.codec.PUBCOMP;
import org.fusesource.mqtt.codec.PUBLISH;
import org.fusesource.mqtt.codec.PUBREC;
import org.fusesource.mqtt.codec.PUBREL;

public class MQTTFlightTracer extends Tracer {

	private static final Logger logger = LogManager.getLogger(MQTTFlightTracer.class);

	private ConcurrentLinkedQueue<MQTTFlightStat> flightStats = new ConcurrentLinkedQueue<MQTTFlightStat>();

	@Override
	public void onSend(MQTTFrame frame) {
		MQTTFlightStat data;

		if (frame.messageType() == PUBLISH.TYPE) {
			data = new MQTTFlightStat();

			try {
				PUBLISH pub = new PUBLISH().decode(frame);

				String[] idPair = Payload.extractIdPair(pub.payload().toByteArray());
				data.messageId = idPair[0] + "-" + idPair[1];

				data.flightId = pub.messageId();
				data.currentTimeMillis = System.currentTimeMillis();
				data.nanoTime = System.nanoTime();
				data.type = PUBLISH.TYPE;
				flightStats.add(data);

			} catch (ProtocolException e) {
				logger.error(e);
			}

		} else if (frame.messageType() == PUBREL.TYPE) {
			data = new MQTTFlightStat();

			try {
				data.flightId = new PUBREL().decode(frame).messageId();
				data.nanoTime = System.nanoTime();
				data.type = PUBREL.TYPE;
				flightStats.add(data);

			} catch (ProtocolException e) {
				logger.error(e);
			}
		}
	}

	@Override
	public void onReceive(MQTTFrame frame) {
		MQTTFlightStat data;

		if (frame.messageType() == PUBACK.TYPE) {
			data = new MQTTFlightStat();

			try {
				data.flightId = new PUBACK().decode(frame).messageId();
				data.nanoTime = System.nanoTime();
				data.type = PUBACK.TYPE;
				flightStats.add(data);

			} catch (ProtocolException e) {
				logger.error(e);
			}

		} else if (frame.messageType() == PUBREC.TYPE) {
			data = new MQTTFlightStat();

			try {
				data.flightId = new PUBREC().decode(frame).messageId();
				data.nanoTime = System.nanoTime();
				data.type = PUBREC.TYPE;
				flightStats.add(data);

			} catch (ProtocolException e) {
				logger.error(e);
			}

		} else if (frame.messageType() == PUBCOMP.TYPE) {
			data = new MQTTFlightStat();

			try {
				data.flightId = new PUBCOMP().decode(frame).messageId();
				data.nanoTime = System.nanoTime();
				data.type = PUBCOMP.TYPE;
				flightStats.add(data);

			} catch (ProtocolException e) {
				logger.error(e);
			}
		}
	}

	public List<MQTTFlightData> getFlightData() {
		Map<Short, MQTTFlightData> flightIdMap = new HashMap<Short, MQTTFlightData>();
		List<MQTTFlightData> flightData = new ArrayList<MQTTFlightData>();

		for (MQTTFlightStat s : this.flightStats) {
			MQTTFlightData d = flightIdMap.get(s.flightId);
			boolean newFlightData = false;

			if (d == null) {
				newFlightData = true;

			} else {
				if (s.type == PUBLISH.TYPE) {
					if (d.getPubTime() != -1) {
						newFlightData = true;
					}
				} else if (s.type == PUBREL.TYPE) {
					if (d.getPubRelSendTime() != -1) {
						newFlightData = true;
					}
				} else if (s.type == PUBACK.TYPE) {
					if (d.getPubAckReceiveTime() != -1) {
						newFlightData = true;
					}
				} else if (s.type == PUBREC.TYPE) {
					if (d.getPubRecReceiveTime() != -1) {
						newFlightData = true;
					}
				} else if (s.type == PUBCOMP.TYPE) {
					if (d.getPubCompReceiveTime() != -1) {
						newFlightData = true;
					}
				}
			}

			if (newFlightData) {
				d = new MQTTFlightData();
				flightIdMap.put(s.flightId, d);
				flightData.add(d);
			}

			if (s.type == PUBLISH.TYPE) {
				d.setMessageId(s.messageId);
				d.setPubTimeMillis(s.currentTimeMillis);
				d.setPubTime(s.nanoTime);

			} else if (s.type == PUBREL.TYPE) {
				d.setPubRelSendTime(s.nanoTime);

			} else if (s.type == PUBACK.TYPE) {
				d.setPubAckReceiveTime(s.nanoTime);

			} else if (s.type == PUBREC.TYPE) {
				d.setPubRecReceiveTime(s.nanoTime);

			} else if (s.type == PUBCOMP.TYPE) {
				d.setPubCompReceiveTime(s.nanoTime);
			}
		}

		return flightData;
	}

	private static final class MQTTFlightStat {

		private short flightId;
		private String messageId;
		private long currentTimeMillis;
		private long nanoTime;
		private byte type;

	}

	public static byte[] toCsv(Collection<MQTTFlightData> flightData) {
		StringBuilder sb = new StringBuilder("MessageId,PubTimeMillis,PubTime,PubAckReceiveTime,PubRecReceiveTime,PubRelSendTime,PubCompReceiveTime\n");

		for (MQTTFlightData temp : flightData) {
			sb.append(temp.getMessageId());
			sb.append(",");
			sb.append(Long.toString(temp.getPubTimeMillis()));
			sb.append(",");
			sb.append(Long.toString(temp.getPubTime()));
			sb.append(",");
			sb.append(Long.toString(temp.getPubAckReceiveTime()));
			sb.append(", ");
			sb.append(Long.toString(temp.getPubRecReceiveTime()));
			sb.append(", ");
			sb.append(Long.toString(temp.getPubRelSendTime()));
			sb.append(", ");
			sb.append(Long.toString(temp.getPubCompReceiveTime()));
			sb.append("\n");
		}

		return sb.toString().getBytes();
	}

}
