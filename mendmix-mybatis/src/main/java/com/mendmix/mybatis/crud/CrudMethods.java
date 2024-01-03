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
package com.mendmix.mybatis.crud;

/**
 * 定义按主键增删改查的方法名
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年9月16日
 */
public enum CrudMethods {
	selectByPrimaryKey,
	deleteByPrimaryKey,
	insert,
	insertSelective,
	insertList,
	updateByPrimaryKey,
	updateByPrimaryKeySelective,
	updateListByPrimaryKeys,
	updateListByPrimaryKeysSelective,
	selectAll,
	selectByPrimaryKeys,
	selectByExample,
	countAll,
	countByExample,
	deleteByPrimaryKeys
	;
}
