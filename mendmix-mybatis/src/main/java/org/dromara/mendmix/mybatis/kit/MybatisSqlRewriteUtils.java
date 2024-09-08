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
package org.dromara.mendmix.mybatis.kit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.exception.MainErrorType;
import org.dromara.mendmix.common.util.BeanUtils;
import org.dromara.mendmix.common.util.CachingFieldUtils;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.pagination.PageSqlUtils;
import org.dromara.mendmix.mybatis.plugin.rewrite.RewriteTable;
import org.dromara.mendmix.mybatis.plugin.rewrite.SqlRewriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.update.Update;

/**
 * mybatis SQL重写工具
 * 
 * <br>
 * Class Name   : MybatisSqlRewriteUtils
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Dec 23, 2020
 */
public class MybatisSqlRewriteUtils {
	
	private static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");
	
	private static final String[] SQL_LINE_CHARS = new String[] { "\r", "\n", "\t" };
	private static final String[] SQL_LINE_REPLACE_CHARS = new String[] { " ", " ", " " };
	private static String mybatisWhereExprEnd = "</where>";
	public static String sqlWherePatternString = "(<|\\s+)WHERE|where(>|\\s+)";
	public static Pattern sqlWherePattern = Pattern.compile(sqlWherePatternString);
	private static Pattern sqlCleanPattern = Pattern.compile("[ \\t\\r\\n]{2,}");
	private static Pattern tablePattern = Pattern.compile("\\s+(FROM|JOIN)\\s+.+?(?=\\s{1})",Pattern.CASE_INSENSITIVE);
	private static Pattern anyTagPattern = Pattern.compile("<.*?(?=>)>",Pattern.CASE_INSENSITIVE);
	private static Pattern nestSelectPattern = Pattern.compile("\\(\\s{0,}(SELECT)\\s+",Pattern.CASE_INSENSITIVE);
	public static final String SQL_PARAMETER_PLACEHOLDER = "?";
	
	public static String cleanSql(String sql) {
		sql = sqlCleanPattern.matcher(sql).replaceAll(StringUtils.SPACE);
		return sql;
	}
	
	public static String toSelectPkFieldSql(SqlCommandType sqlType,String sql,String idColumnName) {
		sql = sql.trim().replaceAll("\n", " ");
		String selectSql = null;
		StringBuilder sqlBuiler = new StringBuilder().append("SELECT ").append(idColumnName).append(" ");
		if(SqlCommandType.DELETE == sqlType) {
			selectSql = sqlBuiler.append(sql.substring(6)).toString();	
		}else if(SqlCommandType.UPDATE == sqlType) {
			String[] segs = sql.split("\\s+",3);
			sqlBuiler.append(" FROM ").append(segs[1]);
			segs = sqlWherePattern.split(sql,2);
			if(segs.length == 2) {
				String maybeWhereEnd = segs[1].substring(segs[1].length() - mybatisWhereExprEnd.length()).toLowerCase();
				if(mybatisWhereExprEnd.equals(maybeWhereEnd)) {
					sqlBuiler.append(" <where>").append(segs[1]);
				}else {
					sqlBuiler.append(" WHERE ").append(segs[1]);
				}
				
			}
			selectSql = sqlBuiler.toString();
		}
		
		return selectSql;
	}
	
	public static SqlMetadata rewriteAsSelectPkField(String sql,String idColumnName) {
		try {
			sql = MybatisSqlRewriteUtils.cleanSql(sql);
			Statement statement = CCJSqlParserUtil.parse(sql);
			
			Table table = null;
			Expression where = null;
			int startIndex = 0;
			int endIndex = 0;
			if(statement instanceof Update) {
				Update update = (Update) statement;
				table = update.getTable();
				where = update.getWhere();
				startIndex = StringUtils.countMatches(update.getExpressions().toString(), SQL_PARAMETER_PLACEHOLDER);
				endIndex = startIndex;
			}else if(statement instanceof Delete) {
				Delete delete = (Delete) statement;
				table = delete.getTable();
				where = delete.getWhere();
			}else {
				return null;
			}
			
			PlainSelect selectBody = new PlainSelect();
			selectBody.setFromItem(table);
			SelectExpressionItem selectItem = new SelectExpressionItem(new Column(idColumnName));
			selectBody.setSelectItems(Arrays.asList(selectItem));
			selectBody.setWhere(where);
			Select select = new Select();
			select.setSelectBody(selectBody);
			
			String rewriteSql = selectBody.toString();
			while(rewriteSql.contains(SQL_PARAMETER_PLACEHOLDER)) {
				rewriteSql = StringUtils.replaceOnce(rewriteSql, SQL_PARAMETER_PLACEHOLDER, "{x}");
				endIndex++;
			}
            //
			rewriteSql = StringUtils.replace(rewriteSql, "{x}", SQL_PARAMETER_PLACEHOLDER);
			return new SqlMetadata(rewriteSql, table.getName(),startIndex, endIndex - 1);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	} 
	
	
	public static List<String> parseSqlUseTables(String sql){
		List<String> tables = new ArrayList<>(3);
		try {
			String cleanSql = StringUtils.replaceEach(sql, SQL_LINE_CHARS, SQL_LINE_REPLACE_CHARS).trim();
			cleanSql = anyTagPattern.matcher(cleanSql).replaceAll("").trim();
			Matcher matcher = tablePattern.matcher(cleanSql);
			String table;
			while(matcher.find()) {
				if(matcher.group().contains("("))continue;
				table = matcher.group().trim().split("\\s+")[1].trim();
				if(!tables.contains(table)) {
					tables.add(table);
				}
			}
		} catch (Exception e) {
			System.err.println("--------------------------------------\n"+sql);
		}
		
		return tables;
	}
	
	public static void parseDyncQueryParameters(BoundSql boundSql,SqlMetadata sqlMetadata) throws Exception {
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		Object parameterObject = boundSql.getParameterObject();
		ParameterMapping parameterMapping;
		
        if(parameterMappings.size() == 1) {
        	setSqlMetadataParamer(sqlMetadata, parameterMappings.get(0), parameterObject);
        }else {
        	Object indexValue = null;
        	for (int i = sqlMetadata.getWhereParameterStartIndex(); i <= sqlMetadata.getWhereParameterEndIndex(); i++) {
    			parameterMapping = parameterMappings.get(i);
    			if(parameterMapping.getProperty().startsWith(SqlRewriteHandler.FRCH_PREFIX)) {
    				indexValue = boundSql.getAdditionalParameter(parameterMapping.getProperty());
    				sqlMetadata.getParameters().add(indexValue);
    			}else {
    				setSqlMetadataParamer(sqlMetadata, parameterMapping, parameterObject);
    			}
    		}
        }
	}
	
	private final static String parseSqlTraceName = "CCJSqlParserUtil.parse";
	public static Statement parseSql(String sql) {
		Statement statement = null;
		Exception ex = null;
		try {
			statement = CCJSqlParserUtil.parse(sql);
		} catch (Exception e) {
			String cleanSql = MybatisSqlRewriteUtils.cleanSql(sql);
			try {
				statement = CCJSqlParserUtil.parse(cleanSql);
			} catch (Exception e1) {
				ex = e1;
				logger.error(">> PARSER_SQL_ERROR \n -sql:{} -\n -reason:{}",sql,ExceptionUtils.getRootCauseStackTrace(e1));
			}
		}
		return statement;
	}
	
	public static String rewriteTableNames(String sql,Map<String, String> rewriteTableNameMapping) {
		try {
			Statement statement = parseSql(sql);
			Table table = null;
			if(statement instanceof Select) {
				PlainSelect select = (PlainSelect)((Select)statement).getSelectBody();
				table = (Table)select.getFromItem();
			}else if(statement instanceof Update) {
				table = ((Update) statement).getTable();
			}else if(statement instanceof Delete) {
				table = ((Delete) statement).getTable();
			}else if(statement instanceof Insert) {
				table = ((Insert) statement).getTable();
			}else {
				throw new MendmixBaseException(MainErrorType.NOT_SUPPORT);
			}
			String tableName = StringUtils.remove(table.getName(), RewriteTable.nameDelimiter);
		    if(rewriteTableNameMapping.containsKey(tableName)) {
		    	table.setName(rewriteTableNameMapping.get(tableName));
		    	sql = statement.toString();
		    }
		    return sql;
		} catch (Exception e) {
			throw new MendmixBaseException(500, "重写sql异常", e);
		}
		
	}
	
	public static boolean withWhereConditions(String sql) {
		sql = cleanSql(sql);
		String[] parts = sqlWherePattern.split(sql,2);
		if(parts.length == 1 || StringUtils.isBlank(parts[1])) {
			//嵌套sql
			String selectHead = PageSqlUtils.matchTopSelectFrom(sql);
			String removeSelectHead = sql.substring(selectHead.length());
			if(nestSelectPattern.matcher(removeSelectHead).find()) {
				String innerSql = PageSqlUtils.matchOutterParenthesesPair(removeSelectHead);
			    return withWhereConditions(innerSql);
			}
			return false;
		}
		//1=1
		String removeSpaceWhere = StringUtils.remove(parts[1].trim(), StringUtils.SPACE);
		return !removeSpaceWhere.substring(0, 2).equals("1=");
	}
	
	private static void setSqlMetadataParamer(SqlMetadata sqlMetadata,ParameterMapping parameterMapping,Object parameterObject) {
		final String propertyName = parameterMapping.getProperty();
		String[] parts = StringUtils.split(propertyName, GlobalConstants.DOT);
		Object value = parameterObject;
		for (String part : parts) {
			if(value instanceof Map) {
				value = ((Map)value).get(part);
			}else if(!BeanUtils.isSimpleDataType(value)){
				value = CachingFieldUtils.readField(value, part);
			}
		}
		if(value != null && value.getClass().isEnum()) {
			value = value.toString();
		}
		sqlMetadata.getParameters().add(value);
	}
	
	public static List<Object> dynaQueryConditionToIdList(DataSource dataSource,OnceContextVal invocationVal) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try {
			BoundSql boundSql = invocationVal.getMappedStatement().getBoundSql(invocationVal.getParameter());
			String orignSql = boundSql.getSql();
			String idColumn = invocationVal.getEntityInfo().getIdColumn();
			SqlMetadata sqlMetadata = MybatisSqlRewriteUtils.rewriteAsSelectPkField(orignSql, idColumn);
			
			MybatisSqlRewriteUtils.parseDyncQueryParameters(boundSql, sqlMetadata);
			connection = dataSource.getConnection();
			String sql = sqlMetadata.getSql();
			Map<String, String> rewriteTableNameMapping = invocationVal.getTableNameMapping();
			if(rewriteTableNameMapping != null && !rewriteTableNameMapping.isEmpty()) {
				sql = MybatisSqlRewriteUtils.rewriteTableNames(sql, rewriteTableNameMapping);
			}
			if(CurrentRuntimeContext.isDebugMode()) {
				logger.info("<debug_trace_logging> dynaQueryConditionIdList sql:{}",sql);
			}
			statement = connection.prepareStatement(sql);
			
			List<Object> parameters = sqlMetadata.getParameters();
			for (int i = 0; i < parameters.size(); i++) {
				statement.setObject(i+1, parameters.get(i));
			}
			
			rs = statement.executeQuery();
			List<Object> ids = new ArrayList<>();
			while (rs.next()) {
				ids.add(rs.getObject(1));
			}
			return ids;
		} catch (Exception e) {
			logger.error("dynaQueryDeleteIdList_ERROR",e);
			return new ArrayList<>(0);
		}finally {
			try {rs.close();} catch (Exception e2) {}
			try {statement.close();} catch (Exception e2) {}
			try {connection.close();} catch (Exception e2) {}
		}
	}

	public static class SqlMetadata {
		String sql;
		String tableName;
		int whereParameterStartIndex;
		int whereParameterEndIndex;
		List<Object> parameters;
		
		
		public SqlMetadata(String sql, String tableName, int whereParameterStartIndex, int whereParameterEndIndex) {
			super();
			this.sql = sql;
			this.tableName = tableName;
			this.whereParameterStartIndex = whereParameterStartIndex;
			this.whereParameterEndIndex = whereParameterEndIndex;
			parameters = new ArrayList<>(whereParameterEndIndex - whereParameterStartIndex);
		}
		public String getSql() {
			return sql;
		}
		public void setSql(String sql) {
			this.sql = sql;
		}
		
		public String getTableName() {
			return tableName;
		}
		public void setTableName(String tableName) {
			this.tableName = tableName;
		}
		public int getWhereParameterStartIndex() {
			return whereParameterStartIndex;
		}
		public void setWhereParameterStartIndex(int whereParameterStartIndex) {
			this.whereParameterStartIndex = whereParameterStartIndex;
		}
		public int getWhereParameterEndIndex() {
			return whereParameterEndIndex;
		}
		public void setWhereParameterEndIndex(int whereParameterEndIndex) {
			this.whereParameterEndIndex = whereParameterEndIndex;
		}
		public List<Object> getParameters() {
			return parameters;
		}
		public void setParameters(List<Object> parameters) {
			this.parameters = parameters;
		}

		
	}
	
	public static void main(String[] args) throws Exception {
		String fileSql;//org.apache.commons.io.FileUtils.readFileToString(new File("D:\\datas\\1.sql"), StandardCharsets.UTF_8);
		fileSql = "SELECT d.* FROM (SELECT  a.* FROM device a ) d";
		System.out.println(withWhereConditions(fileSql));
		fileSql = "SELECT d.* FROM (SELECT  a.* FROM device a WHERE a.id = '1') d";
		System.out.println(withWhereConditions(fileSql));
		fileSql = "SELECT  a.* FROM device a WHERE a = 1";
		System.out.println(withWhereConditions(fileSql));
	}
}
