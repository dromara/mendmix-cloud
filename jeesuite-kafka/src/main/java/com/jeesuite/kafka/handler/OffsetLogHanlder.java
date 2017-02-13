/**
 * 
 */
package com.jeesuite.kafka.handler;

/**
 * 消息消费偏移量记录器
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年2月10日
 */
public interface OffsetLogHanlder {

	/**
	 * 获取上一次记录的已经处理的偏移量
	 * @param group
	 * @param topic
	 * @param partition
	 * @return
	 */
	long getLatestProcessedOffsets(String group,String topic,int partition);
	
	/**
	 * 处理前记录偏移量
	 * @param group
	 * @param topic
	 * @param partition
	 * @param offset
	 */
	void saveOffsetsBeforeProcessed(String group,String topic,int partition,long offset);
	
	/**
	 * 处理后记录偏移量
	 * @param group
	 * @param topic
	 * @param partition
	 * @param offset
	 */
	void saveOffsetsAfterProcessed(String group,String topic,int partition,long offset);
}
