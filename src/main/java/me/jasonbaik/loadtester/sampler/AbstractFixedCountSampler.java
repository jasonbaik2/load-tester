package me.jasonbaik.loadtester.sampler;

import java.util.Iterator;
import java.util.List;

import me.jasonbaik.loadtester.reporter.Reportable;
import me.jasonbaik.loadtester.valueobject.ReportData;

public abstract class AbstractFixedCountSampler<T1, T2 extends AbstractFixedCountSamplerConfig<?>> extends AbstractSampler<T1, T2> implements Reportable<ReportData> {

	protected abstract void forEach(SamplerTask<T1> samplerTask, List<T1> payloads) throws InterruptedException;

	protected abstract void forEach(SamplerTask<T1> samplerTask, Iterator<T1> Iterator) throws InterruptedException;

	@Override
	public void sample(SamplerTask<T1> samplerTask, List<T1> payloads) throws InterruptedException {
		forEach(samplerTask, payloads);
	}

	@Override
	public void sample(SamplerTask<T1> samplerTask, Iterator<T1> Iterator) throws InterruptedException {
		forEach(samplerTask, Iterator);
	}

}
