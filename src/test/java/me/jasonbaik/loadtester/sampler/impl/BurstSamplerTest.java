package me.jasonbaik.loadtester.sampler.impl;

import java.util.concurrent.TimeUnit;

import me.jasonbaik.loadtester.sampler.PayloadIterator;
import me.jasonbaik.loadtester.sampler.SamplerTask;
import me.jasonbaik.loadtester.sampler.impl.BurstSampler;
import me.jasonbaik.loadtester.sampler.impl.BurstSamplerConfig;

import org.junit.Assert;
import org.junit.Test;

public class BurstSamplerTest {

	private int numRun;

	@Test
	public void testForEach() {
		int interval = 2;
		int burstCount = 10;

		BurstSamplerConfig config = new BurstSamplerConfig();
		config.setBurstCount(burstCount);
		config.setBurstInterval(interval);
		config.setBurstIntervalUnit(TimeUnit.SECONDS);

		BurstSampler burstSampler = new BurstSampler(config);
		burstSampler.forEach(new SamplerTask<byte[]>() {

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

		});

		Assert.assertEquals(burstCount, numRun);
	}

	@Test
	public void testDuring() {
		int duration = 10;
		int interval = 2;
		int burstCount = 10;

		BurstSamplerConfig config = new BurstSamplerConfig();
		config.setBurstCount(burstCount);
		config.setBurstInterval(interval);
		config.setBurstIntervalUnit(TimeUnit.SECONDS);

		BurstSampler burstSampler = new BurstSampler(config);
		burstSampler.during(new SamplerTask<byte[]>() {

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

		Assert.assertEquals(duration / interval * burstCount, numRun);
	}

}
