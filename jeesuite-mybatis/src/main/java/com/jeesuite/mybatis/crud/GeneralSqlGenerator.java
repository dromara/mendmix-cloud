/**
 * 
 */
package com.jeesuite.mybatis.crud;

import java.util.List;

import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.crud.builder.DeleteBuilder;
import com.jeesuite.mybatis.crud.builder.GetByPrimaryKeyBuilder;
import com.jeesuite.mybatis.crud.builder.InsertBuilder;
import com.jeesuite.mybatis.crud.builder.UpdateBuilder;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;

/**
 * @description <br>
 * @author <a href="mailto:wei.jiang@lifesense.com">vakin</a>
 * @date 2016年3月24日
 * @Copyright (c) 2015, lifesense.com
 */
public class GeneralSqlGenerator {

	public static void generate(Configuration configuration) {
		List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos();
		for (EntityInfo entity : entityInfos) {
			GetByPrimaryKeyBuilder.build(configuration, entity);
			InsertBuilder.build(configuration, entity);
			UpdateBuilder.build(configuration, entity);
			DeleteBuilder.build(configuration, entity);
		}
	}

	

	
}
