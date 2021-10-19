package com.jeesuite.mybatis.kit;

import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

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
	
	public static final String SQL_PARAMETER_PLACEHOLDER = "?";
	
	public static SqlMetadata rewriteAsSelectPkField(String sql,String idColumnName) {
		try {
			Statement statement = CCJSqlParserUtil.parse(sql);
			
			Table table = null;
			Expression where = null;
			int startIndex = 1;
			if(statement instanceof Update) {
				Update update = (Update) statement;
				table = update.getTable();
				where = update.getWhere();
				startIndex = StringUtils.countMatches(update.getExpressions().toString(), SQL_PARAMETER_PLACEHOLDER) + 1;
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
				rewriteSql = StringUtils.replaceOnce(rewriteSql, SQL_PARAMETER_PLACEHOLDER, "{" + startIndex + "}");
				startIndex++;
			}
            
			int parameterNums = StringUtils.countMatches(rewriteSql, SQL_PARAMETER_PLACEHOLDER);
			return new SqlMetadata(rewriteSql, table.getName(),parameterNums, startIndex);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	} 
	

	public static class SqlMetadata {
		String sql;
		String tableName;
		int parameterNums;
		int whereParameterIndex;
		
		public SqlMetadata(String sql,String tableName, int parameterNums, int whereParameterIndex) {
			this.sql = sql;
			this.tableName = tableName;
			this.parameterNums = parameterNums;
			this.whereParameterIndex = whereParameterIndex;
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
		public int getParameterNums() {
			return parameterNums;
		}
		public void setParameterNums(int parameterNums) {
			this.parameterNums = parameterNums;
		}
		public int getWhereParameterIndex() {
			return whereParameterIndex;
		}
		public void setWhereParameterIndex(int whereParameterIndex) {
			this.whereParameterIndex = whereParameterIndex;
		}
	}
	
	public static void main(String[] args) throws SQLException {
		SqlMetadata sql = rewriteAsSelectPkField("update users set status = ?,enabled=1 where id in ( ? , ? , ? ) and status = '0'","id");
		System.out.println(sql.getSql());
	}
}
