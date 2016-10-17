package me.jasonbaik.loadtester.util;

import java.io.IOException;
import java.util.Scanner;

import org.junit.Test;

public class RandomJSONGeneratorTest {

	@Test
	public void test() throws IOException {
		try (Scanner scanner = new Scanner(getClass().getResourceAsStream("template.json"))) {
			scanner.useDelimiter("$^");

			System.out.println(new String(new RandomJSONGenerator(scanner.next()).generate()));
		}
	}

}
