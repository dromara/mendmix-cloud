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
package com.mendmix.mybatis.crud.builder;

import org.apache.ibatis.jdbc.SQL;

import com.mendmix.mybatis.metadata.EntityMetadata;
import com.mendmix.mybatis.metadata.TableMetadata;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月2日
 * @Copyright (c) 2015, jwww
 */
public class SelectAllBuilder  extends AbstractSelectMethodBuilder{

	@Override
	String[] methodNames() {
		return new String[]{"selectAll"};
	}

	@Override
	String buildSQL(EntityMetadata entityMapper, boolean selective) {
		TableMetadata tableMapper = entityMapper.getTable();
		return new SQL() {
            {
                SELECT("*");
                FROM(tableMapper.getName());
            }
        }.toString();
	}
	
	@Override
	boolean scriptWrapper() {
		return false;
	}

}
