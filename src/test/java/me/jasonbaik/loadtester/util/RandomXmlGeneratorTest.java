package me.jasonbaik.loadtester.util;

import static org.junit.Assert.assertEquals;

import javax.xml.bind.JAXBException;

import me.jasonbaik.loadtester.util.RandomXmlGenerator;

import org.junit.Test;

public class RandomXmlGeneratorTest {

	@Test
	public void benchmark() {
		int noOfMessages = 100;
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < noOfMessages; i++) {
			try {
				RandomXmlGenerator.generate(100);
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		long endTime = System.currentTimeMillis();
		long timeTaken = endTime - startTime;
		System.out.println("Time taken to create " + noOfMessages + " messages: " + timeTaken + " ms");
	}

	@Test
	public void test() throws JAXBException {
		int baseXmlCount = 334;
		int count = baseXmlCount + 100;
		int byteCount = RandomXmlGenerator.generate(100).length;
		assertEquals(count, byteCount);
	}

}
