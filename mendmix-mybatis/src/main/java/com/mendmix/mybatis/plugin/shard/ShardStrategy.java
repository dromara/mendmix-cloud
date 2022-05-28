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
package com.mendmix.mybatis.plugin.shard;

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
