package me.jasonbaik.loadtester.util;

import static org.junit.Assert.assertEquals;

import javax.xml.bind.JAXBException;

import org.junit.Test;

public class RandomXmlGeneratorTest {

	@Test
	public void benchmark() {
		int noOfMessages = 100;
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < noOfMessages; i++) {
			try {
				RandomXmlGenerator.generate(1024);
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		long endTime = System.currentTimeMillis();
		long timeTaken = endTime - startTime;
		System.out.println("Time taken to create " + noOfMessages + " 1k messages: " + timeTaken + " ms");
	}

	@Test
	public void test() throws JAXBException {
		int length = 1024;
		assertEquals(length, RandomXmlGenerator.generate(length).length);
	}

}
