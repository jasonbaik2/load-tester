package me.jasonbaik.loadtester.util;

import java.util.Random;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.RandomStringUtils;

@XmlRootElement(name = "PhotoCollection")
public class XmlItem {
	@XmlElement
	private Item item;

	public XmlItem() {

	}

	public XmlItem(int size) {
		item = new Item(size);
	}

	private static class Item {
		@XmlElement
		private String name;
		@XmlElement
		private String description;
		@XmlElement
		private String where;
		@XmlElement
		private Position position;
		private int totalSize;

		private Item(int size) {
			if (size > 0) {
				this.totalSize = (int) (0.75 * size);

				if ((size - totalSize) > 0)
					position = new Position(size - totalSize);
				generateRandomName((int) (0.3 * totalSize));
				generateRandomDescription((int) (0.3 * totalSize));
				generateRandomWhere(totalSize - (int) (0.6 * totalSize));
			}

		}

		private void generateRandomName(int size) {
			name = RandomStringUtils.randomAlphanumeric(size);
		}

		private void generateRandomDescription(int size) {
			description = RandomStringUtils.randomAlphanumeric(size);
		}

		private void generateRandomWhere(int size) {
			where = RandomStringUtils.randomAlphanumeric(size);
		}

		private static class Position {
			@XmlElement
			private int srsDimension;
			@XmlElement
			private String srsName = "";
			@XmlElement
			private String pos;
			private int totalSize;
			Random rand;

			protected Position(int size) {
				if (size > 0) {
					rand = new Random();
					totalSize = size - 1;
					generateSrsDimension();
					if (totalSize > 0) {
						generatePos();
						totalSize--;
						if (totalSize > 0)
							generateSrsName(totalSize);
					}

				}
			}

			private void generateSrsDimension() {
				srsDimension = Math.abs(rand.nextInt() % 3);
			}

			private void generateSrsName(int size) {
				srsName = RandomStringUtils.randomAlphanumeric(size);
			}

			private void generatePos() {
				pos = "" + Math.abs(rand.nextInt() % 10);
			}
		}
	}

}
