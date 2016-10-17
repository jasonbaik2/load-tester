package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import me.jasonbaik.loadtester.sampler.SamplerFactory;

public class FixedCountCyclicSamplerTest {

	private int numRun;

	@Test
	public void testForEach() throws Exception {
		int interval = 500;
		final int count = 10;

		FixedCountCyclicSamplerConfig config = new FixedCountCyclicSamplerConfig();
		config.setInterval(interval);
		config.setIntervalUnit(TimeUnit.MILLISECONDS);

		SamplerFactory.newInstance(config).forEach((index, payload) -> {
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
				if (numRun == count) {
					return false;
				}
				return true;
			}

		});

		Assert.assertEquals(count, numRun);
	}

}
