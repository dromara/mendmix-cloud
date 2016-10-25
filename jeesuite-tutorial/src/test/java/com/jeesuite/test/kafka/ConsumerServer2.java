package com.jeesuite.test.kafka;

import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class ConsumerServer2 {
	
	private static Logger logger = LoggerFactory.getLogger(ConsumerServer2.class);
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
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
			@Override
			public void run() {
//				Map stats = KafkaMonitor.getContext().stats("kafka-demo");
//		        System.out.println("\n===========MONITOR DATA============\n" + stats + "\n===========MONITOR DATA============\n");
			}
		}, 3000,15000);
        
        Scanner scan=new Scanner(System.in); 
		String cmd=scan.next();
		if("q".equalsIgnoreCase(cmd)){
			context.close();
			scan.close();
			timer.cancel();
		}

    }
}
