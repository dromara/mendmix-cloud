package com.jeesuite.test;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.jeesuite.kafka.consumer.ConsumerContext;


public class ConsumerServer {
	
	private static Logger logger = LoggerFactory.getLogger(ConsumerServer.class);
    public static void main(String[] args) throws InterruptedException{

        final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("test-kafka-consumer.xml");

        logger.info("Kafka Consumer started....");
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			
			@Override
			public void run() {
			   logger.info("Kafka Consumer Stoped....");
			   context.close();
			}
		}));

        
        Scanner scan=new Scanner(System.in); 
		String cmd=scan.next();
		if("q".equalsIgnoreCase(cmd)){
			context.close();
			scan.close();
		}else if("stopfetch".equalsIgnoreCase(cmd)){
			ConsumerContext.getInstance().switchFetch(true);
		}

    }
}
