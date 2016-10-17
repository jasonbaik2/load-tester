package me.jasonbaik.loadtester.tests;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.beans.BeansException;

import me.jasonbaik.loadtester.sender.Sender;
import me.jasonbaik.loadtester.sender.SenderFactory;
import me.jasonbaik.loadtester.valueobject.Send;

public class SendTest {

	private static final Logger logger = LogManager.getLogger(SendTest.class);

	public static void main(String[] args) throws BeansException, Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(args[0]);
		Send<?> send = context.getBean(Send.class);
		Sender<?> sender = SenderFactory.newInstance(send.getSenderConfig());

		ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();

		es.scheduleWithFixedDelay(() -> {
			sender.log();
		}, 0, 2, TimeUnit.SECONDS);

		logger.info("Start sending using " + send);

		sender.send();
		context.close();
	}

}
