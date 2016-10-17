package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import me.jasonbaik.loadtester.sampler.SamplerFactory;

public class FixedDurationCyclicSamplerTest {

	private int numRun;

	@Test
	public void testDuring() throws Exception {
		int duration = 5;
		int interval = 1;

		FixedDurationCyclicSamplerConfig config = new FixedDurationCyclicSamplerConfig();
		config.setInterval(interval);
		config.setIntervalUnit(TimeUnit.SECONDS);

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

		Assert.assertEquals(duration / interval, numRun);
	}

}
