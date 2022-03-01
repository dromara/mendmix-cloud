/**
 * 
 */
package com.jeesuite.mybatis.plugin.shard;

/**
 * 数据库分库策略接口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年2月2日
 * @Copyright (c) 2015, jwww
 */
public interface ShardStrategy<T> {

	/**
	 * 分库字段
	 * @return
	 */
	public String shardDbField();
	
	/**
	 * 分库字段对应实体属性名
	 * @return
	 */
	public String shardEntityField();
	
	
	
	/**
	 * 分配逻辑
	 * @param value
	 * @return 数据库index
	 */
	public int sharding(Object value);

}
