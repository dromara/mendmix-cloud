/**
 * 
 */
package org.dromara.mendmix.logging.tracelog.processor;

import java.util.List;

import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.logging.LogKafkaClient;
import org.dromara.mendmix.logging.tracelog.ChainTraceContext;
import org.dromara.mendmix.logging.tracelog.TraceLogProcessor;
import org.dromara.mendmix.logging.tracelog.TraceSpan;

/**
 * <br>
 * @author vakinge
 * @date 2024年5月7日
 */
public class KafkaApmLogProcessor implements TraceLogProcessor {

	private String topicName = ResourceUtils.getProperty("mendmix-cloud.logging.tracelog.topicName", "mendmix_topic_traceLog");
	private int batchSize = ResourceUtils.getInt("mendmix-cloud.logging.tracelog.batchSize", 20);

	private LogKafkaClient kafkaClient;
	
	public KafkaApmLogProcessor(LogKafkaClient kafkaClient) {
		this.kafkaClient = kafkaClient;
		ChainTraceContext.setLogProcessor(this);
	}

	@Override
	public void process(TraceSpan traceSpan) {
		List<TraceSpan> list = traceSpan.toTileList();
		
		List<TraceSpan> subList;
		int toIndex = 0;
		for (int i = 0; i < list.size(); i+=batchSize) {
			if(list.size() <= batchSize) {
				subList = list;				
			}else {
				toIndex = (i + batchSize) > list.size() ? list.size() : (i + batchSize);
				subList = list.subList(i, toIndex);
			}
			kafkaClient.send(topicName, JsonUtils.toJson(subList));
		}
	}
	

}
