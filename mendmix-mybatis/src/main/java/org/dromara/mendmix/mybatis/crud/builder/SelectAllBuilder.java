/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.mybatis.crud.builder;

import org.apache.ibatis.jdbc.SQL;
import org.dromara.mendmix.mybatis.metadata.EntityMetadata;
import org.dromara.mendmix.mybatis.metadata.TableMetadata;

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
		TableMetadata tableMapper = entityMapper.getTableMapper();
		return new SQL() {
            {
                SELECT("*");
                FROM(tableMapper.getName());
            }
        }.toString();
	}

}
