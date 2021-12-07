package com.jeesuite.mybatis.kit;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.SqlCommandType;

import com.jeesuite.mybatis.metadata.SqlMetadata;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.Join;
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
	
	private static String mybatisWhereExprEnd = "</where>";
	public static String sqlWherePatternString = "(<|\\s+)WHERE|where(>|\\s+)";
	public static Pattern sqlWherePattern = Pattern.compile(sqlWherePatternString);
	
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
			String cleanSql = sql.replace("<where>", " where ").replace("<WHERE>", " WHERE ");
			cleanSql = StringUtils.replacePattern(cleanSql, "<.*>"," ");
			cleanSql = StringUtils.replacePattern(cleanSql, "(\\$|\\#){1}.*\\}", "1").trim();
			if(cleanSql.toLowerCase().endsWith(" where")) {
				cleanSql = cleanSql.substring(0,cleanSql.length() - 5).trim();
			}
			Select select = (Select) CCJSqlParserUtil.parse(cleanSql);
			PlainSelect selectBody = (PlainSelect) select.getSelectBody();
			Table table = (Table) selectBody.getFromItem();
			tables.add(table.getName().toLowerCase());
			List<Join> joins = selectBody.getJoins();
			if (joins != null) {
				for (Join join : joins) {
					table = (Table) join.getRightItem();
					tables.add(table.getName().toLowerCase());
				}
			}
		} catch (Exception e) {}
		
		return tables;
	}
	
	public static void main(String[] args) throws SQLException {
		String sql = "select * from users 	 <where>    <if test=\"name != null\">          AND name = #{name}      </if>    <if test=\"mobile != null\">          AND mobile = #{mobile}      </if></where>";
		
		System.out.println(parseSqlUseTables(sql));
	}
}
