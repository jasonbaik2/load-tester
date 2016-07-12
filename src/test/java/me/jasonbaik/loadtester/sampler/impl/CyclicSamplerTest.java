package me.jasonbaik.loadtester.sampler.impl;

import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sampler.PayloadIterator;
import me.jasonbaik.loadtester.sampler.SamplerTask;
import me.jasonbaik.loadtester.sampler.impl.CyclicSampler;
import me.jasonbaik.loadtester.sampler.impl.CyclicSamplerConfig;

import org.junit.Assert;
import org.junit.Test;

public class CyclicSamplerTest {

	private int numRun;

	@Test
	public void testForEach() {
		int interval = 500;
		final int count = 10;

		CyclicSamplerConfig config = new CyclicSamplerConfig();
		config.setInterval(interval);
		config.setIntervalUnit(TimeUnit.MILLISECONDS);

		CyclicSampler cyclicSampler = new CyclicSampler(config);
		cyclicSampler.forEach(new SamplerTask<byte[]>() {

			@Override
			public void run(int index, byte[] payload) throws Exception {
				System.out.println("Sample #" + index + " running");
				numRun++;
			}

		}, new PayloadIterator<byte[]>() {

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

	@Test
	public void testDuring() {
		int duration = 5;
		int interval = 1;

		CyclicSamplerConfig config = new CyclicSamplerConfig();
		config.setInterval(interval);
		config.setIntervalUnit(TimeUnit.SECONDS);

		CyclicSampler cyclicSampler = new CyclicSampler(config);
		cyclicSampler.during(new SamplerTask<byte[]>() {

			@Override
			public void run(int index, byte[] payload) throws Exception {
				System.out.println("Sample #" + index + " running");
				numRun++;
			}

		}, new PayloadIterator<byte[]>() {

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

		}, duration, TimeUnit.SECONDS);

		Assert.assertEquals(duration / interval, numRun);
	}

}
