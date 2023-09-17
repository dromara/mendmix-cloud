package com.mendmix.amqp;

import java.util.Date;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

import com.mendmix.spring.InstanceFactory;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年8月15日
 */
public class MessageStateCheckService {


	private static String countSql = "SELECT COUNT(1) FROM transaction_message_log WHERE id = ?";
	private static String insertSql = "INSERT INTO `transaction_message_log` (`id`,`topic`, `msg_id`,`created_at`, `created_by`) VALUES (?,?,?,?,?)";
	
	private JdbcTemplate jdbcTemplate;
    

	public JdbcTemplate getJdbcTemplate() {
		if(jdbcTemplate != null)return jdbcTemplate;
		synchronized (this) {
			Map<String, JdbcTemplate> beans = InstanceFactory.getBeansOfType(JdbcTemplate.class);
			if(beans.size() == 1) {
				jdbcTemplate = beans.values().stream().findFirst().get();
			}else {
				jdbcTemplate = beans.get("defaultJdbcTemplate");
			}
		}
		return jdbcTemplate;
	}

	public void saveMessageTx(MQMessage message) {
		if(getJdbcTemplate() == null)return;
		jdbcTemplate.update(insertSql, message.getTxId(),message.getTopic(),message.getMsgId(),new Date(),message.getProduceBy());
	}
	
	public boolean checkMessageTx(String txId) {
		if(getJdbcTemplate() == null)return true;
		return jdbcTemplate.queryForObject(countSql, Integer.class, txId) > 0;
	}

}
