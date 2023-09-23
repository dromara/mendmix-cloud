/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.common2.sequence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.mendmix.cache.CacheExpires;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.common2.jdbc.DataSourceGroups;
import com.mendmix.common2.jdbc.JdbcExecutor;
import com.mendmix.common2.lock.redis.RedisDistributeLock;
import com.mendmix.common2.task.SubTimerTask;

/**
 * 
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年6月1日
 */
public class SequenceGenerateService implements SubTimerTask, InitializingBean {

	private static Logger logger = LoggerFactory.getLogger("com.mendmix.common2");

	private static long taskInterval = RandomUtils.nextLong(5000, 10000);

	private static Duration queuePopWaiting = Duration.ofMillis(taskInterval + 10000);

	private static final String NONE_TIME_EXPRE = "none";
	private static final String SEQUENCE_QUEUE_KEY_TPL = "sequence.queue:%s_%s";
	private static final String SEQUENCE_QUEUE_LAST_SEQ_KEY_TPL = "sequence.queue.lastSeq:%s";

	private static String[] paddingzeros = new String[] { "", "0", "00", "000", "0000", "00000", "000000", "0000000",
			"00000000", "000000000" };

	private static SequenceGenerateService me;

	@Autowired(required = false)
	@Qualifier("defaultDataSource")
	private DataSource defaultDataSource;

	@Value("${sequence.code.prefix:}")
	private String codePrefix;

	private List<String> matchCodes = ResourceUtils.getList("sequence.code.scopes");

	@Value("${sequence.batch.produce.count:200}")
	private int batchSize;
	private int threshold;

	@Autowired
	private StringRedisTemplate redisTemplate;

	private JdbcExecutor jdbcExecutor;

	private boolean noRuleTable = false;

	private AtomicReference<Map<String, SequenceRule>> sequenceRules = new AtomicReference<>();

	public static String next(String code) {
		if (me == null) {
			throw new MendmixBaseException("SequenceGenerateService not init");
		}
		return me.genSequence(code);
	}

	public String genSequence(String code) {
		SequenceRule rule = findRuleByCode(code);
		String timeSequence = NONE_TIME_EXPRE;
		if (StringUtils.isNotBlank(rule.getTimeExpr())) {
			timeSequence = buildTimeExprSequence(rule.getTimeExpr());
		}
		String queueName = String.format(SEQUENCE_QUEUE_KEY_TPL, code, timeSequence);
		if (redisTemplate.opsForList().size(queueName) < threshold) {
			generateWithLock(rule, queueName, timeSequence);
		}
		final String item = redisTemplate.opsForList().rightPop(queueName, queuePopWaiting);
		return item;
	}

	private void procuceSequenceAndAddQueue(SequenceRule rule, String timeSequence, String queueName) {

		int hasCount = redisTemplate.opsForList().size(queueName).intValue();
		int produceNums = batchSize - hasCount;

		if (produceNums <= threshold)
			return;
		// 是否跨天重置
		boolean isReset = isCrossPeriodReset(rule, queueName);
		RedisDistributeLock resetLck = null;
		String resetMarkKey = null;
		if (isReset) {
			resetMarkKey = "queueName.reset:" + queueName;
			resetLck = new RedisDistributeLock(resetMarkKey);
			resetLck.lock();
			// 并发情况重新判断一次
			isReset = !redisTemplate.hasKey(resetMarkKey);
		}
		logger.info(">>>procuceSequence begin -> code:{},queueName:{},isReset:{},produceNums:{}", rule.getCode(),
				queueName, isReset, produceNums);
		List<String> sequeueList = new ArrayList<>(produceNums);
		int[] incrSeqRange = updateLastSequenceValue(rule, produceNums, isReset);

		StringBuilder builder = new StringBuilder();
		if (StringUtils.isNotBlank(rule.getPrefix())) {
			builder.append(rule.getPrefix());
		}
		if (StringUtils.isNotBlank(rule.getTimeExpr())) {
			builder.append(timeSequence);
		}

		int prefixLength = builder.length();
		String seq;
		for (int i = incrSeqRange[0]; i < incrSeqRange[1]; i++) {
			builder.setLength(prefixLength);
			seq = String.valueOf(i);
			int len = rule.getSeqLength() - seq.length();
			if (len > 0) {
				seq = paddingzeros[len] + seq;
			}
			builder.append(seq);
			//
			if (rule.getRandomLength() > 0) {
				builder.append(RandomStringUtils.random(rule.getRandomLength(), RandomType.chars(rule.getRandomType()),
						RandomType.numbers(rule.getRandomType())));
			}
			sequeueList.add(builder.toString());
		}

		redisTemplate.opsForList().leftPushAll(queueName, sequeueList);
		redisTemplate.expire(queueName, Duration.ofSeconds(CacheExpires.todayEndSeconds() + 5));
		// 记录当前已经重置过，方式并发重复处理
		if (isReset) {
			redisTemplate.opsForValue().set(resetMarkKey, queueName);
			redisTemplate.expire(resetMarkKey, Duration.ofSeconds(30));
		}
		logger.info(">>>procuceSequence end -> code:{},queueName:{},isReset:{},incrRange:{}~{}", rule.getCode(),
				queueName, isReset, incrSeqRange[0], incrSeqRange[1]);
	}

	private SequenceRule findRuleByCode(String code) {
		if (StringUtils.isBlank(code)) {
			throw new MendmixBaseException("编码不能为空");
		}
		SequenceRule sequenceRule = code == null ? null
				: (sequenceRules.get() == null ? null : sequenceRules.get().get(code));
		if (sequenceRule == null) {
			sequenceRule = jdbcExecutor.queryForObject("SELECT * FROM sequence_rules WHERE code=?",
					new Object[] { code }, SequenceRule.class);
		}
		return sequenceRule;
	}

	/**
	 * buildTimeExprSequence
	 * 
	 * @param timeExpr
	 *            timeExpr
	 * @return String
	 */
	private String buildTimeExprSequence(String timeExpr) {
		String seq = timeExpr;
		Calendar calendar = Calendar.getInstance();
		if (timeExpr.contains(SeqTimeExpr.YEAR.getExpr())) {
			seq = seq.replace(SeqTimeExpr.YEAR.getExpr(), String.valueOf(calendar.get(Calendar.YEAR)));
		} else if (timeExpr.contains(SeqTimeExpr.SHORT_YEAR.getExpr())) {
			String value = String.valueOf(calendar.get(Calendar.YEAR)).substring(2);
			seq = seq.replace(SeqTimeExpr.SHORT_YEAR.getExpr(), value);
		}
		if (timeExpr.contains(SeqTimeExpr.MONTH.getExpr())) {
			int month = calendar.get(Calendar.MONTH) + 1;
			seq = seq.replace(SeqTimeExpr.MONTH.getExpr(), month > 9 ? String.valueOf(month) : ("0" + month));
		}
		if (timeExpr.contains(SeqTimeExpr.DAY.getExpr())) {
			int date = calendar.get(Calendar.DATE);
			seq = seq.replace(SeqTimeExpr.DAY.getExpr(), date > 9 ? String.valueOf(date) : ("0" + date));
		}
		// 忽略时分秒
		if (timeExpr.length() > 14) {
			if (timeExpr.contains(SeqTimeExpr.HOUR.getExpr())) {
				seq = seq.replace(SeqTimeExpr.HOUR.getExpr(), StringUtils.EMPTY);
			}
			if (timeExpr.contains(SeqTimeExpr.MINUTE.getExpr())) {
				seq = seq.replace(SeqTimeExpr.MINUTE.getExpr(), StringUtils.EMPTY);
			}
			if (timeExpr.contains(SeqTimeExpr.SECOND.getExpr())) {
				seq = seq.replace(SeqTimeExpr.SECOND.getExpr(), StringUtils.EMPTY);
			}
		}
		return seq;
	}

	private int[] updateLastSequenceValue(SequenceRule rule, int produceNums, boolean reset) {
		int startSeq;
		int endSeq;
		String cacheKey = String.format(SEQUENCE_QUEUE_LAST_SEQ_KEY_TPL, rule.getCode());
		if (reset) {
			startSeq = rule.getFirstSequence();
			endSeq = rule.getFirstSequence() + produceNums;
			redisTemplate.opsForValue().set(cacheKey, String.valueOf(endSeq));
		} else {
			endSeq = redisTemplate.opsForValue().increment(cacheKey, produceNums).intValue();
			startSeq = endSeq - produceNums;
			if (startSeq == 0)
				startSeq = 1;
		}
		logger.info(">> updateLastSequenceValue ->code:{},reset:{},startSeq:{},endSeq:{}", rule.getCode(), reset,
				startSeq, endSeq);
		//
		updateDbLastSequenceValue(rule, endSeq);

		return new int[] { startSeq, endSeq };
	}

	private boolean updateDbLastSequenceValue(SequenceRule rule, int updateLastSeq) {
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			connection = jdbcExecutor.getconnnection();
			connection.setAutoCommit(true);

			statement = connection
					.prepareStatement("UPDATE sequence_rules SET last_sequence=?,updated_at=? WHERE id=?");
			statement.setInt(1, updateLastSeq);
			statement.setTimestamp(2, new Timestamp(Calendar.getInstance().getTimeInMillis()));
			statement.setString(3, rule.getId());
			//
			int res = statement.executeUpdate();
			return res > 0;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			jdbcExecutor.close(null, statement, connection);
		}
	}

	@Override
	public void doSchedule() {
		if (noRuleTable)
			return;
		String sql = "SELECT * FROM sequence_rules WHERE enabled = 1";
		if (StringUtils.isNotBlank(codePrefix)) {
			sql = sql + " AND code like '" + codePrefix + "%'";
		}
		List<SequenceRule> rules = null;
		try {
			rules = jdbcExecutor.queryForList(sql, null, SequenceRule.class);
		} catch (Exception e) {
			noRuleTable = (e.getMessage() != null && e.getMessage().contains("doesn't exist"));
			return;
		}

		final Map<String, SequenceRule> ruleMapping = new HashMap<>(rules.size());
		for (SequenceRule rule : rules) {
			if (matchCodes.isEmpty() || matchCodes.contains(rule.getCode())) {
				ruleMapping.put(rule.getCode(), rule);
			}
		}
		sequenceRules.set(ruleMapping);
		//
		String queueName;
		for (SequenceRule rule : ruleMapping.values()) {
			String timeSequence = NONE_TIME_EXPRE;
			if (StringUtils.isNotBlank(rule.getTimeExpr())) {
				timeSequence = buildTimeExprSequence(rule.getTimeExpr());
			}
			queueName = String.format(SEQUENCE_QUEUE_KEY_TPL, rule.getCode(), timeSequence);
			//
			generateWithLock(rule, queueName, timeSequence);
		}

	}

	private void generateWithLock(SequenceRule rule, String queueName, String timeSequence) {
		RedisDistributeLock lock = null;
		boolean getLocked = false;
		try {
			lock = new RedisDistributeLock("sequence.queue:" + rule.getCode());
			if (getLocked = lock.tryLock()) {
				procuceSequenceAndAddQueue(rule, timeSequence, queueName);
			}
		} finally {
			if (getLocked)
				lock.unlock();
		}
	}

	/**
	 * 是否跨天重置
	 * 
	 * @param rule
	 * @param queueName
	 * @return
	 */
	private boolean isCrossPeriodReset(SequenceRule rule, String queueName) {
		if (StringUtils.isBlank(rule.getTimeExpr())) {
			return false;
		}
		Calendar calendar = Calendar.getInstance();
		if (rule.getTimeExpr().endsWith(SeqTimeExpr.MONTH.getExpr())) {
			if (calendar.get(Calendar.DAY_OF_MONTH) != calendar.getActualMinimum(Calendar.DAY_OF_MONTH)) {
				return false;
			}
		} else if (rule.getTimeExpr().endsWith(SeqTimeExpr.YEAR.getExpr())) {// 按年
			if (calendar.get(Calendar.DAY_OF_YEAR) != calendar.getActualMinimum(Calendar.DAY_OF_YEAR)) {
				return false;
			}
		}
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		boolean isCrossPeriod = hour == 0 && minute <= 1;
		if (isCrossPeriod) {
			isCrossPeriod = !redisTemplate.hasKey(queueName);
		}
		return isCrossPeriod;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		me = this;
		DataSource dataSource;
		try {
			dataSource = DataSourceGroups.getDataSource("sequence");
		} catch (MendmixBaseException e) {
			dataSource = defaultDataSource;
		}
		jdbcExecutor = new JdbcExecutor(dataSource);
		//
		threshold = batchSize * 20 / 100;

		logger.info(">>>>>SequenceGenerateService inited -> taskInterval:{},batchSize:{},threshold:{}", taskInterval,
				batchSize, threshold);

	}

	@Override
	public long interval() {
		return taskInterval;
	}

}
