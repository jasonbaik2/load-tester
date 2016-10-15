package me.jasonbaik.loadtester.tests;

import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.beans.BeansException;

import me.jasonbaik.loadtester.sender.Sender;

public class SendTest {

	public static void main(String[] args) throws BeansException, Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(args[0]);
		Sender<?> sender = context.getBean(Sender.class);
		sender.init();
		sender.send();
		context.close();
	}

}
