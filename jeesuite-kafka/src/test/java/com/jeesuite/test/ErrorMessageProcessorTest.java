/**
 * 
 */
package com.jeesuite.test;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import com.jeesuite.kafka.consumer.ErrorMessageDefaultProcessor;
import com.jeesuite.kafka.message.DefaultMessage;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月25日
 */
public class ErrorMessageProcessorTest {

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		ErrorMessageDefaultProcessor processor = new ErrorMessageDefaultProcessor();
		
		Demo2MessageHandler messageHandler = new Demo2MessageHandler();
		while(true){			
			processor.submit(new DefaultMessage(RandomStringUtils.random(5, true, true)), messageHandler);
			Thread.sleep(RandomUtils.nextLong(100, 500));
		}
	}

}
