package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import me.jasonbaik.loadtester.sampler.SamplerFactory;

public class FixedDurationRandomSamplerTest {

	private int numRun;

	@Test
	public void testDuring() throws Exception {
		int interval = 100;
		int duration = 5000;

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
				return true;
			}

		});

		long endTime = System.currentTimeMillis();

		Assert.assertTrue(numRun > duration / interval * 0.8 && numRun < duration / interval * 1.2);
		Assert.assertTrue(endTime - startTime > duration * 0.8 && endTime - startTime < duration * 1.2);
	}

}
