/**
 * 
 */
package com.jeesuite.monitor.web.controller;

import java.util.List;

import com.jeesuite.kafka.monitor.KafkaMonitor;
import com.jeesuite.kafka.monitor.model.ConsumerGroupInfo;
import com.jfinal.core.Controller;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月28日
 */
//@Before(AuthInterceptor.class)
public class KafkaController extends Controller {

	public void brokers() {
		setAttr("brokers", KafkaMonitor.getContext().getAllBrokers());
		render("brokers.html");
	}

	public void consumers() {
		List<ConsumerGroupInfo> groupInfos = KafkaMonitor.getContext().getAllConsumerGroupInfos();
        setAttr("consumerGroups", groupInfos);
		render("consumers.html");
	}
	
	public void producers() {
		render("producers.html");
	}

}
