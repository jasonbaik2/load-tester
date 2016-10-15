package me.jasonbaik.loadtester.sampler.impl;

import java.util.concurrent.TimeUnit;

import java.util.Iterator;
import me.jasonbaik.loadtester.sampler.SamplerTask;

import org.junit.Assert;
import org.junit.Test;

public class RandomSamplerTest {

	private int numRun;

	@Test
	public void testForEach() throws InterruptedException {
		int interval = 100;
		final int count = 50;

		FixedCountRandomSamplerConfig config = new FixedCountRandomSamplerConfig();
		config.setExpectedInterval(interval);
		config.setExpectedIntervalUnit(TimeUnit.MILLISECONDS);

		FixedCountRandomSampler RandomSampler = new FixedCountRandomSampler(config);

		long startTime = System.currentTimeMillis();

		RandomSampler.forEach((index, payload) -> {
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

	@Test
	public void testDuring() throws InterruptedException {
		int interval = 100;
		int duration = 5000;

		FixedCountRandomSamplerConfig config = new FixedCountRandomSamplerConfig();
		config.setExpectedInterval(interval);
		config.setExpectedIntervalUnit(TimeUnit.MILLISECONDS);

		FixedCountRandomSampler RandomSampler = new FixedCountRandomSampler(config);

		long startTime = System.currentTimeMillis();

		RandomSampler.during((index, payload) -> {
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

		}, duration, TimeUnit.MILLISECONDS);

		long endTime = System.currentTimeMillis();

		Assert.assertTrue(numRun > duration / interval * 0.8 && numRun < duration / interval * 1.2);
		Assert.assertTrue(endTime - startTime > duration * 0.8 && endTime - startTime < duration * 1.2);
	}

}
