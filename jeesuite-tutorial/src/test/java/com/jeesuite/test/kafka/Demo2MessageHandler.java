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
public class Demo2MessageHandler implements MessageHandler {


	@Override
	public void p1Process(DefaultMessage message) {
		//TODO 第一阶段处理，譬如一些需要及时
	}

	@Override
	public void p2Process(DefaultMessage message) {
		//第二阶段处理一些耗时操作，如：最终入库
		Serializable body = message.getBody();
		System.out.println("Demo2MessageHandler process message:" + body);
		try {Thread.sleep(100);} catch (Exception e) {}
	}


	@Override
	public boolean onProcessError(DefaultMessage message) {
		//如果业务自己处理消费错误的消息，再这里实现并return false;
		return false;
	}

}
