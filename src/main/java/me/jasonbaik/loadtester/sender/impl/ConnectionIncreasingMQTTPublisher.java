package me.jasonbaik.loadtester.sender.impl;

import java.util.ArrayList;

import me.jasonbaik.loadtester.sampler.Sampler;
import me.jasonbaik.loadtester.sender.Sender;
import me.jasonbaik.loadtester.valueobject.ReportData;

public class ConnectionIncreasingMQTTPublisher extends Sender<byte[], ConnectionIncreasingMQTTPublisherConfig> {

	public ConnectionIncreasingMQTTPublisher(ConnectionIncreasingMQTTPublisherConfig config) {
		super(config);
	}

	@Override
	public ArrayList<ReportData> report() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(Sampler<byte[], ?> sampler) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
