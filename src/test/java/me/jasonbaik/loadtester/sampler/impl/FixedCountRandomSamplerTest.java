package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import me.jasonbaik.loadtester.sampler.SamplerFactory;

public class FixedCountRandomSamplerTest {

	private int numRun;

	@Test
	public void testForEach() throws Exception {
		int interval = 100;
		final int count = 50;

		FixedCountRandomSamplerConfig config = new FixedCountRandomSamplerConfig();
		config.setExpectedInterval(interval);
		config.setExpectedIntervalUnit(TimeUnit.MILLISECONDS);

		long startTime = System.currentTimeMillis();

		SamplerFactory.newInstance(config).sample((index, payload) -> {
			System.out.println("Sample #" + index + " running");
			numRun++;
		}, new Iterator<byte[]>() {

			@Override
			public void remove() {
				// TODO Auto-generated method stub

			}

			@Override
			public byte[] next() {
				return new byte[] { 'a' };
			}

			@Override
			public boolean hasNext() {
				if (count == numRun) {
					return false;
				}
				return true;
			}

		});

		long endTime = System.currentTimeMillis();

		Assert.assertEquals(count, numRun);
		Assert.assertTrue(endTime - startTime > interval * count * 0.8 && endTime - startTime < interval * count * 1.2);
	}

}
