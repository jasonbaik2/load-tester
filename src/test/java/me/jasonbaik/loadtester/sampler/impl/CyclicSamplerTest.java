package me.jasonbaik.loadtester.sampler.impl;

import java.util.concurrent.TimeUnit;

import java.util.Iterator;
import me.jasonbaik.loadtester.sampler.SamplerTask;
import me.jasonbaik.loadtester.sampler.impl.FixedCountCyclicSampler;
import me.jasonbaik.loadtester.sampler.impl.FixedCountCyclicSamplerConfig;

import org.junit.Assert;
import org.junit.Test;

public class CyclicSamplerTest {

	private int numRun;

	@Test
	public void testForEach() throws InterruptedException {
		int interval = 500;
		final int count = 10;

		FixedCountCyclicSamplerConfig config = new FixedCountCyclicSamplerConfig();
		config.setInterval(interval);
		config.setIntervalUnit(TimeUnit.MILLISECONDS);

		FixedCountCyclicSampler cyclicSampler = new FixedCountCyclicSampler(config);
		cyclicSampler.forEach((index, payload) -> {
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

	@Test
	public void testDuring() throws InterruptedException {
		int duration = 5;
		int interval = 1;

		FixedCountCyclicSamplerConfig config = new FixedCountCyclicSamplerConfig();
		config.setInterval(interval);
		config.setIntervalUnit(TimeUnit.SECONDS);

		FixedCountCyclicSampler cyclicSampler = new FixedCountCyclicSampler(config);
		cyclicSampler.during((index, payload) -> {
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

		}, duration, TimeUnit.SECONDS);

		Assert.assertEquals(duration / interval, numRun);
	}

}
