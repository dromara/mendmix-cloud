package com.jeesuite.test;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class SimpleComsumer {

	public static void main(String[] args) {
		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("group.id", "simpleComsumer2");
		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", "1000");
		props.put("session.timeout.ms", "30000");
		props.put("auto.offset.reset", "earliest");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.LongDeserializer");
		KafkaConsumer<String, Long> consumer = new KafkaConsumer<>(props);
		/* 消费者订阅的topic, 可同时订阅多个 */
		consumer.subscribe(Arrays.asList("streams-wordcount-output"));
		 
		 /* 读取数据，读取超时时间为100ms */
		while (true) {
		    ConsumerRecords<String, Long> records = consumer.poll(1000);
		    for (ConsumerRecord<String, Long> record : records)
		        System.out.printf("offset = %d, key = %s, value = %s \n", record.offset(), record.key(), record.value());
		}

	}
}
