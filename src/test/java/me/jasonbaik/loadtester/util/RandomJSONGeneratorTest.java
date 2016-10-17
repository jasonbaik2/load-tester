package me.jasonbaik.loadtester.util;

import java.io.IOException;

import org.junit.Test;

public class RandomJSONGeneratorTest {

	@Test
	public void test() throws IOException {
		System.out.println(new String(new RandomJSONGenerator(getClass().getResourceAsStream("template.json")).generate()));
	}

}
