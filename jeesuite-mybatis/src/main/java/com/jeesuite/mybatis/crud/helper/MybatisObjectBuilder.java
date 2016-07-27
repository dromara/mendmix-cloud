/**
 * 
 */
package com.jeesuite.mybatis.crud.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.scripting.xmltags.MixedSqlNode;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.scripting.xmltags.StaticTextSqlNode;
import org.apache.ibatis.session.Configuration;

/**
 * @description <br>
 * @author <a href="mailto:wei.jiang@lifesense.com">vakin</a>
 * @date 2016年3月24日
 * @Copyright (c) 2015, lifesense.com
 */
public class MybatisObjectBuilder {

	public static SqlNode generateSqlNode(String sql) {

		List<SqlNode> sqlNodes = new ArrayList<SqlNode>();
		sqlNodes.add(new StaticTextSqlNode(sql));
		SqlNode sqlNode = new MixedSqlNode(sqlNodes);

		return sqlNode;
	}
	
	
	/**
     * 生成当前实体的resultMap对象
     *
     * @param configuration
     * @return
     */
	public static ResultMap getResultMap(Configuration configuration,Class<?> entityClass) {
        List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
        
        Set<ColumnMapper> entityClassColumns = EntityHelper.getEntityMapper(entityClass).getColumnsMapper();
        for (ColumnMapper entityColumn : entityClassColumns) {
            ResultMapping.Builder builder = new ResultMapping.Builder(configuration, entityColumn.getProperty(), entityColumn.getColumn(), entityColumn.getJavaType());
            if (entityColumn.getJdbcType() != null) {
                builder.jdbcType(entityColumn.getJdbcType());
            }

            List<ResultFlag> flags = new ArrayList<ResultFlag>();
            if (entityColumn.isId()) {
                flags.add(ResultFlag.ID);
            }
            builder.flags(flags);
            builder.lazy(false);
            resultMappings.add(builder.build());
        }
        ResultMap.Builder builder = new ResultMap.Builder(configuration, "BaseResultMap", entityClass, resultMappings, true);
        return builder.build();
    }
}
