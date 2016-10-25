/**
 * 
 */
package com.jeesuite.test.kafka;

import java.io.Serializable;

import com.jeesuite.kafka.handler.MessageHandler;
import com.jeesuite.kafka.message.DefaultMessage;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年6月25日
 */
public class DemoMessageHandler implements MessageHandler {


	@Override
	public void p1Process(DefaultMessage message) {
		//TODO 
	}

	@Override
	public void p2Process(DefaultMessage message) {
		Serializable body = message.getBody();
		System.out.println("DemoMessageHandler process message:" + body);
		try {Thread.sleep(100);} catch (Exception e) {}
	}


	@Override
	public boolean onProcessError(DefaultMessage message) {
		System.out.println("ignore error message : "+message);
		return true;
	}

}
