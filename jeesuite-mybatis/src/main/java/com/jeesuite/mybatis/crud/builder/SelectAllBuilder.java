/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

import org.apache.ibatis.jdbc.SQL;

import com.jeesuite.mybatis.metadata.EntityMetadata;
import com.jeesuite.mybatis.metadata.TableMetadata;

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
