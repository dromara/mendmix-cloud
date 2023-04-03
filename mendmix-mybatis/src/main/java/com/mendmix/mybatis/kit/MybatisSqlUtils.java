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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.SqlCommandType;

import com.mendmix.mybatis.metadata.SqlMetadata;

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
	private static Pattern tagPattern = Pattern.compile("<.*?(?=>)>",Pattern.CASE_INSENSITIVE);
	private static Pattern withTablePattern = Pattern.compile("\\s+(FROM|JOIN)\\s+.+?(?=\\s+)",Pattern.CASE_INSENSITIVE);
	public static final String SQL_PARAMETER_PLACEHOLDER = "?";
	
	
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
			cleanSql = cleanSql.replaceAll("\\s{2,}", " ");
			cleanSql = tagPattern.matcher(cleanSql).replaceAll("");
			Matcher matcher = withTablePattern.matcher(cleanSql);
			while(matcher.find()) {
				String table = matcher.group().trim().split("\\s+")[1].trim();
				if(!tables.contains(table)) {
					tables.add(table);
				}
			}
		} catch (Exception e) {
			System.err.println("parseSqlUseTableError:"+sql);
		}
		
		return tables;
	}
	
	public static String underscoreToCamelCase(String para){
		if(!para.contains("_"))return para.toLowerCase();
        StringBuilder result=new StringBuilder();
        String a[]=para.toLowerCase().split("_");
        for(String s:a){
            if(result.length()==0){
                result.append(s.toLowerCase());
            }else{
                result.append(s.substring(0, 1).toUpperCase());
                result.append(s.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
	
	public static void main(String[] args) throws SQLException {
		String sql = "select * from users 	 <where>    <if test=\"name != null\">          AND name = #{name}      </if>    <if test=\"mobile != null\">          AND mobile = #{mobile}      </if></where>";
		sql = "select * from users 	\nWHERE 1=1 \n<if test=\"name != null\">          AND name = #{name}      </if>    \n<if test=\"mobile != null\">          AND mobile = #{mobile}      </if>"
		+ "\nAND role_id IN \n<foreach collection=\"roleIds\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\">#{id}</foreach> ORDER BY id ";
		System.out.println(parseSqlUseTables(sql));
		
	}
}
