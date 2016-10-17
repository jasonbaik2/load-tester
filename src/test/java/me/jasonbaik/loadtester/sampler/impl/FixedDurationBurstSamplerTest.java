package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import me.jasonbaik.loadtester.sampler.SamplerFactory;

public class FixedDurationBurstSamplerTest {

	private int numRun;

	@Test
	public void testDuring() throws Exception {
		int duration = 10;
		int interval = 2;
		int burstCount = 10;

		FixedDurationBurstSamplerConfig config = new FixedDurationBurstSamplerConfig();
		config.setBurstCount(burstCount);
		config.setBurstInterval(interval);
		config.setBurstIntervalUnit(TimeUnit.SECONDS);

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

		Assert.assertEquals(duration / interval * burstCount, numRun);
	}

}
