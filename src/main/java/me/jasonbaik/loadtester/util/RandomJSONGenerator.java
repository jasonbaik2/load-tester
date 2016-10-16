package me.jasonbaik.loadtester.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;

public class RandomJSONGenerator {

	private static final Pattern tokenPattern = Pattern.compile("(.*):(.*)");

	private List<Field> fields = new LinkedList<Field>();

	public RandomJSONGenerator(String jsonTemplate) {
		parseTemplate(jsonTemplate);
	}

	public void parseTemplate(String jsonTemplate) {
		try (Scanner scanner = new Scanner(jsonTemplate)) {
			scanner.useDelimiter("%");

			while (scanner.hasNext()) {
				String token = scanner.next();
				Matcher m = tokenPattern.matcher(token);

				if (m.matches()) {
					// Add the current match
					FieldType type = FieldType.valueOf(m.group(1));
					String[] metaTokens = m.group(2).split(",");

					switch (type) {
					case DOUBLE:
						fields.add(new DoubleField(Double.parseDouble(metaTokens[0]), Double.parseDouble(metaTokens[1]), Integer.parseInt(metaTokens[2])));
						break;
					case INT:
						fields.add(new IntField(Integer.parseInt(metaTokens[0]), Integer.parseInt(metaTokens[1])));
						break;
					case STRING:
						fields.add(new StringField(Integer.parseInt(metaTokens[0]), Integer.parseInt(metaTokens[1])));
						break;
					case ID:
						fields.add(new IdField());
						break;
					case TIMESTAMP:
						fields.add(new TimestampField());
						break;
					default:
						throw new IllegalArgumentException("Unknown field type: " + m.group(1));
					}
				} else {
					fields.add(new StaticField(token));
				}
			}
		}
	}

	public byte[] generate() throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		for (Field f : fields) {
			os.write(f.getValue().getBytes(Charset.forName("UTF-8")));
		}

		return os.toByteArray();
	}

	static enum FieldType {
		ID, TIMESTAMP, INT, DOUBLE, STRING;
	}

	static interface Field {
		String getValue();

	}

	static class StaticField implements Field {

		String value;

		StaticField(String value) {
			super();
			this.value = value;
		}

		@Override
		public String getValue() {
			return value;
		}

	}

	static class IdField implements Field {

		ThreadLocal<Integer> id = new ThreadLocal<Integer>() {

			@Override
			protected Integer initialValue() {
				return 0;
			}

		};

		@Override
		public String getValue() {
			int next = id.get() + 1;
			id.set(next);
			return Integer.toString(next);
		}

	}

	static class TimestampField implements Field {

		@Override
		public String getValue() {
			return Long.toString(System.currentTimeMillis());
		}

	}

	static class IntField implements Field {

		int min;
		int max;
		int span;

		public IntField(int min, int max) {
			super();
			this.min = min;
			this.max = max;
			this.span = max - min + 1;
		}

		@Override
		public String getValue() {
			return Integer.toString(ThreadLocalRandom.current().nextInt(span) + min);
			// FIXME Handle arithmetic overflow
		}

	}

	static class DoubleField implements Field {

		double min;
		double max;
		int precision;

		public DoubleField(double min, double max, int precision) {
			this.min = min;
			this.max = max;
			this.precision = precision;
		}

		@Override
		public String getValue() {
			int num = (int) ((max - min + 1) * Math.pow(10, precision));
			return Double.toString(ThreadLocalRandom.current().nextInt(num) / Math.pow(10, precision));
			// FIXME Handle arithmetic overflow
		}

	}

	static class StringField implements Field {

		int minLength;
		int maxLength;

		public StringField(int minLength, int maxLength) {
			super();
			this.minLength = minLength;
			this.maxLength = maxLength;
		}

		@Override
		public String getValue() {
			return RandomStringUtils.random(minLength + ThreadLocalRandom.current().nextInt(maxLength - minLength + 1));
			// Handle arithmetic overflow
		}

	}

}