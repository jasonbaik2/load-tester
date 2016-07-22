package me.jasonbaik.loadtester.sampler.impl;

import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sampler.PayloadIterator;
import me.jasonbaik.loadtester.sampler.SamplerTask;
import me.jasonbaik.loadtester.sampler.impl.RandomSampler;
import me.jasonbaik.loadtester.sampler.impl.RandomSamplerConfig;

import org.junit.Assert;
import org.junit.Test;

public class RandomSamplerTest {

	private int numRun;

	@Test
	public void testForEach() {
		int interval = 100;
		final int count = 50;

		RandomSamplerConfig config = new RandomSamplerConfig();
		config.setExpectedInterval(interval);
		config.setExpectedIntervalUnit(TimeUnit.MILLISECONDS);

		RandomSampler RandomSampler = new RandomSampler(config);

		long startTime = System.currentTimeMillis();

		RandomSampler.forEach(new SamplerTask<byte[]>() {

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

	@Test
	public void testDuring() {
		int interval = 100;
		int duration = 5000;

		RandomSamplerConfig config = new RandomSamplerConfig();
		config.setExpectedInterval(interval);
		config.setExpectedIntervalUnit(TimeUnit.MILLISECONDS);

		RandomSampler RandomSampler = new RandomSampler(config);

		long startTime = System.currentTimeMillis();

		RandomSampler.during(new SamplerTask<byte[]>() {

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

		}, duration, TimeUnit.MILLISECONDS);

		long endTime = System.currentTimeMillis();

		Assert.assertTrue(numRun > duration / interval * 0.8 && numRun < duration / interval * 1.2);
		Assert.assertTrue(endTime - startTime > duration * 0.8 && endTime - startTime < duration * 1.2);
	}

}
