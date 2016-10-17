package me.jasonbaik.loadtester.sampler.impl;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import me.jasonbaik.loadtester.sampler.SamplerFactory;

public class FixedCountBurstSamplerTest {

	private int numRun;

	@Test
	public void testForEach() throws Exception {
		int burstCount = 10;

		FixedCountBurstSamplerConfig config = new FixedCountBurstSamplerConfig();
		config.setCount(burstCount);

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

		Assert.assertEquals(burstCount, numRun);
	}

}
