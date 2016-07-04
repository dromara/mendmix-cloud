package com.jeesuite.cache.command;

import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisCommands;
import static com.jeesuite.cache.redis.JedisProviderFactory.getJedisProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * 数字redis操作命令
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 */
public class RedisNumber extends RedisString {

	public RedisNumber(String key) {
		super(key);
	}

	public RedisNumber(String key,String groupName) {
		super(key, groupName);
	}
	
	public boolean set(Object value) {
		return super.set(value.toString());
	}

	public boolean set(Object value, long seconds) {
		return super.set(value.toString(), seconds);
	}
	
	public boolean set(Object value, Date expireAt) {
		return super.set(value.toString(), expireAt);
	}
	
	public Integer getInteger() {
		String value = super.get();
		return value == null ? null : NumberUtils.toInt(value);
	}
	
	public Long getLong() {
		String value = super.get();
		return value == null ? null : NumberUtils.toLong(value);
	}
	
	public Float getFloat() {
		String value = super.get();
		return value == null ? null : NumberUtils.toFloat(value);
	}
	
	public Short getShort() {
		String value = super.get();
		return value == null ? null : NumberUtils.toShort(value);
	}
	
	public Double getDouble() {
		String value = super.get();
		return value == null ? null : NumberUtils.toDouble(value);
	}

	/**
	 * 
	 * @param scale 小数点位数
	 * @return
	 */
	public BigDecimal getBigDecimal(int scale) {
		String value = super.get();
		return value == null ? null : new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP);
	}
	
	/**
	 * 指定key的值加操作
	 * @param integer
	 * @return
	 */
	public boolean increase(long integer){
		try {
			return getJedisCommands(groupName).incrBy(key, integer) == 1;
		} finally {
			getJedisProvider(groupName).release();
		}
	}
	
	/**
	 * 指定key的值减操作
	 * @param integer
	 * @return
	 */
	public boolean reduce(long integer){
		try {
			return getJedisCommands(groupName).incrBy(key, 0-integer) == 1;
		} finally {
			getJedisProvider(groupName).release();
		}
	}
}
