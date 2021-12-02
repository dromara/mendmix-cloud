package com.jeesuite.amqp.kafka;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.amqp.AbstractProducer;
import com.jeesuite.amqp.MQMessage;
import com.jeesuite.common.util.ResourceUtils;

/**
 * 
 * <br>
 * Class Name   : KafkaMQProducer
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年9月18日
 */
public class KafkaProducerAdapter extends AbstractProducer {

	private final static Logger logger = LoggerFactory.getLogger("com.jeesuite.amqp");
	
	private KafkaProducer<String, Object> kafkaProducer;
	
	@Override
	public void start() throws Exception {
		Properties configs = buildConfigs();
        kafkaProducer = new KafkaProducer<String, Object>(configs);

	}

	@Override
	public String sendMessage(MQMessage message, boolean async) {
		String topic = message.getTopic();
		Integer partition = null; //
		String key = message.getBizKey();
		String value = message.toMessageValue(true);
		List<Header> headers = null;
		
		ProducerRecord<String,Object> producerRecord = new ProducerRecord<String, Object>(topic, partition, key, value, headers);

		if (async) {
			kafkaProducer.send(producerRecord, new Callback(){
				@Override
				public void onCompletion(RecordMetadata recordMetadata, Exception e) {
					message.onProducerFinished(null,recordMetadata.partition(), recordMetadata.offset());
                    if (e == null) {//成功发送
                        handleSuccess(message);
                        logger.debug("发送成功, topic:{}, partition:{}, offset:{}", topic, recordMetadata.partition(), recordMetadata.offset());
                    }else{
                        //发送失败
                        handleError(message, e);
                        logger.warn("发送失败, topic:{}, partition:{}, offset:{}, exception:{}", topic, recordMetadata.partition(), recordMetadata.offset(), e);
                    }
				}
			});
        } else {
            try {
                Future<RecordMetadata> future= kafkaProducer.send(producerRecord);
                RecordMetadata recordMetadata = future.get();
                message.onProducerFinished(null,recordMetadata.partition(), recordMetadata.offset());
                this.handleSuccess(message);
            } catch (Exception e) {
                this.handleError(message, e);
            }
        }
		return null;
	}

	@Override
	public void shutdown() {
		kafkaProducer.close();
	}
	
	private static Properties buildConfigs() {
		
		Properties result = new Properties();

		Class<ProducerConfig> clazz = ProducerConfig.class;
		Field[] fields = clazz.getDeclaredFields();
		String propName;
		String propValue;
		for (Field field : fields) {
			if(!field.getName().endsWith("CONFIG") || field.getType() != String.class)continue;
			field.setAccessible(true);
			try {				
				propName = field.get(clazz).toString();
			} catch (Exception e) {
				continue;
			}
			propValue = ResourceUtils.getProperty("jeesuite.amqp.kafka["+propName+"]");
			if(StringUtils.isNotBlank(propValue)) {
				result.setProperty(propName, propValue);
			}
		}
		
		if (!result.containsKey(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)) {
        	throw new NullPointerException("Kafka config[bootstrap.servers] is required");
        }
		
		if(!result.containsKey(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)){
			result.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()); // key serializer
        }
        
        if(!result.containsKey(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)){
        	result.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        }

        //默认重试一次
        if(!result.containsKey(ProducerConfig.RETRIES_CONFIG)){
        	result.put(ProducerConfig.RETRIES_CONFIG, "1"); 
        }
        
        if(!result.containsKey(ProducerConfig.COMPRESSION_TYPE_CONFIG)){
        	result.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); 
        }
		
		return result;
	}

}
