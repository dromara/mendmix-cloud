package org.dromara.mendmix.amqp;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

import org.dromara.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import org.dromara.mendmix.common.util.DateUtils;
import org.dromara.mendmix.spring.InstanceFactory;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年8月15日
 */
public class MessageStateCheckService implements InitializingBean,DisposableBean{


	private static final int INTERVAL = 60 * 60 * 1000;
	private static String countSql = "SELECT COUNT(1) FROM transaction_message_log WHERE id = ?";
	private static String insertSql = "INSERT INTO `transaction_message_log` (`id`,`topic`, `msg_id`,`created_at`, `created_by`) VALUES (?,?,?,?,?)";
	
	private ScheduledExecutorService executor;
	private JdbcTemplate jdbcTemplate;
    
    private Boolean withLogTable;
    

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
		if(getJdbcTemplate() == null || !isWithLogTable())return;
		jdbcTemplate.update(insertSql, message.getTxId(),message.getTopic(),message.getMsgId(),new Date(),message.getProduceBy());
	}
	
	public boolean checkMessageTx(String txId) {
		if(getJdbcTemplate() == null || !isWithLogTable())return true;
		return jdbcTemplate.queryForObject(countSql, Integer.class, txId) > 0;
	}
	
	private boolean isWithLogTable() {
		return withLogTable != null && withLogTable;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		executor = Executors.newScheduledThreadPool(1,new StandardThreadFactory("MessageStateCheckScheduler"));
		executor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				if(getJdbcTemplate() == null || (withLogTable != null && !withLogTable)) {
					return;
				}
				Date dateEnd = DateUtils.addHours(new Date(), -168);
				try {
					jdbcTemplate.update("DELETE FROM transaction_message_log WHERE created_at < ?",dateEnd);
					withLogTable = true;
				} catch (Exception e) {
					if(e instanceof org.springframework.jdbc.BadSqlGrammarException) {
						withLogTable = false;
					}
				}
			}
		}, 1000, INTERVAL, TimeUnit.MILLISECONDS);
	}

	@Override
	public void destroy() throws Exception {
		executor.shutdown();
	}

}
