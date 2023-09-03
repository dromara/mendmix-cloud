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
package com.mendmix.mybatis.kit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;

import com.mendmix.common.GlobalConstants;
import com.mendmix.common.util.BeanUtils;
import com.mendmix.common.util.CachingFieldUtils;
import com.mendmix.mybatis.metadata.SqlMetadata;
import com.mendmix.mybatis.plugin.rewrite.SqlRewriteHandler;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.update.Update;

/**
 * mybatis SQL工具
 * 
 * <br>
 * Class Name   : MybatisSqlUtils
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Dec 23, 2020
 */
public class MybatisSqlUtils {
	
	private static final String[] SQL_LINE_CHARS = new String[] { "\r", "\n", "\t" };
	private static final String[] SQL_LINE_REPLACE_CHARS = new String[] { " ", " ", " " };
	private static String mybatisWhereExprEnd = "</where>";
	public static String sqlWherePatternString = "(<|\\s+)WHERE|where(>|\\s+)";
	public static Pattern sqlWherePattern = Pattern.compile(sqlWherePatternString);
	private static Pattern sqlCleanPattern = Pattern.compile("[ \\t\\r\\n]{2,}");
	private static Pattern tablePattern = Pattern.compile("\\s+(FROM|JOIN)\\s+.+?(?=\\s{1})",Pattern.CASE_INSENSITIVE);
	private static Pattern anyTagPattern = Pattern.compile("<.*?(?=>)>",Pattern.CASE_INSENSITIVE);
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
			sql = cleanSql(sql);
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
	
	public static void main(String[] args) throws Exception {
		String fileSql;//org.apache.commons.io.FileUtils.readFileToString(new File("D:\\datas\\1.sql"), StandardCharsets.UTF_8);
		fileSql = "SELECT d.* FROM (SELECT  a.* FROM device a ) d";
		System.out.println(parseSqlUseTables(fileSql));
	}
}
