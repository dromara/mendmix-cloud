package com.jeesuite.mybatis.kit;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.SqlCommandType;

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
	
	public static void main(String[] args) throws SQLException {
		String sql = "update users set type = #{type},updated_at = now()     	 <where>    <if test=\"name != null\">          AND name = #{name}      </if>    <if test=\"mobile != null\">          AND mobile = #{mobile}      </if></where>";
		sql = toSelectPkFieldSql(SqlCommandType.UPDATE, sql, "id");
		System.out.println(sql);
	}
}
