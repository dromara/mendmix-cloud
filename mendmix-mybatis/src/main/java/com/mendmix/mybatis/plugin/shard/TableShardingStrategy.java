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

import com.mendmix.mybatis.plugin.InvocationVals;

/**
 * 分表策略接口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Dec 22, 2022
 */
public interface TableShardingStrategy {

	/**
	 * 生成表后缀
	 * @param args mybatis请求参数
	 * @return 数据库index
	 */
	public String buildShardingTableName(String tableName,InvocationVals invocation);

}
