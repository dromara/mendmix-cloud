package com.jeesuite.test;

import com.jeesuite.kafka.handler.OffsetLogHanlder;

public class MysqlOffsetLogHanlder implements OffsetLogHanlder {

	@Override
	public long getLatestProcessedOffsets(String group, String topic, int partition) {
		return -1;
	}

	@Override
	public void saveOffsetsBeforeProcessed(String group, String topic, int partition, long offset) {
		System.out.println(String.format("saveOffsetsBeforeProcessed:topic:%s,partition:%s,offset:%s", topic,partition,offset));
	}

	@Override
	public void saveOffsetsAfterProcessed(String group, String topic, int partition, long offset) {
		System.out.println(String.format("saveOffsetsAfterProcessed:topic:%s,partition:%s,offset:%s", topic,partition,offset));
	}

}
