package me.jasonbaik.loadtester.sampler.impl;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

public class BurstSamplerTest {

	private int numRun;

	@Test
	public void testForEach() throws InterruptedException {
		int interval = 2;
		int burstCount = 10;

		FixedCountBurstSamplerConfig config = new FixedCountBurstSamplerConfig();
		config.setBurstCount(burstCount);
		config.setBurstInterval(interval);
		config.setBurstIntervalUnit(TimeUnit.SECONDS);

		FixedCountBurstSampler burstSampler = new FixedCountBurstSampler(config);
		burstSampler.forEach((index, payload) -> {
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

	@Test
	public void testDuring() throws InterruptedException {
		int duration = 10;
		int interval = 2;
		int burstCount = 10;

		FixedCountBurstSamplerConfig config = new FixedCountBurstSamplerConfig();
		config.setBurstCount(burstCount);
		config.setBurstInterval(interval);
		config.setBurstIntervalUnit(TimeUnit.SECONDS);

		FixedCountBurstSampler burstSampler = new FixedCountBurstSampler(config);
		burstSampler.during((index, payload) -> {
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

		Assert.assertEquals(duration / interval * burstCount, numRun);
	}

}
