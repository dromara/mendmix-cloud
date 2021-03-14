package com.jeesuite.mybatis.kit;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.mybatis.crud.helper.ColumnMapper;
import com.jeesuite.mybatis.crud.helper.EntityHelper;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
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
	
	
	/**
	 * @param orignSql
	 * @param columnDefineList
	 * @param dataMappings
	 * @return
	 */
	public static String buildDataProfileSql(String orignSql, List<ColumnMapper> columnDefineList,Map<String, String[]> dataMapping) {
		Select select = null;
		try {
			select = (Select) CCJSqlParserUtil.parse(orignSql);
		} catch (JSQLParserException e) {
			throw new RuntimeException("sql解析错误");
		}
		
		PlainSelect selectBody = (PlainSelect) select.getSelectBody();
		Table table = (Table) selectBody.getFromItem();
		
		Iterator<ColumnMapper> iterator = columnDefineList.iterator();
		Expression newExpression = null;
		ColumnMapper item;
		while(iterator.hasNext()){
			item =iterator.next();
			if(!dataMapping.containsKey(item.getProperty()))continue;
			newExpression = appendDataProfileCondition(table, selectBody.getWhere(), item.getColumn(),dataMapping.get(item.getProperty()));
			selectBody.setWhere(newExpression);
			//主表已经处理的条件，join表不在处理
			iterator.remove();
		}
		
		//JOIN 
		List<Join> joins = selectBody.getJoins();
		if(joins != null && !columnDefineList.isEmpty()){
			List<ColumnMapper> columns;
			for (Join join : joins) {
				table = (Table) join.getRightItem();
				columns = EntityHelper.getTableColumnMappers(table.getName());
				if(columns == null)continue;
				for (ColumnMapper defineColumn : columnDefineList) {
					if(!columns.contains(defineColumn))continue;
					if(!dataMapping.containsKey(defineColumn.getProperty()))continue;
					newExpression = appendDataProfileCondition(table, join.getOnExpression(), defineColumn.getColumn(),dataMapping.get(defineColumn.getProperty()));
					join.setOnExpression(newExpression);
				}
				
			}
		}
		//
		String newSql = selectBody.toString();
		
		return newSql;
	}
	
	private static Expression appendDataProfileCondition(Table table,Expression orginExpression,String columnName,String[] values){
		Expression newExpression = null;
		Column column = new Column(table, columnName);
		if (values.length == 1) {
			EqualsTo equalsTo = new EqualsTo();
			equalsTo.setLeftExpression(column);
			equalsTo.setRightExpression(new StringValue(values[0]));
			newExpression = orginExpression == null ? equalsTo : new AndExpression(orginExpression, equalsTo);
		} else {
			ExpressionList expressionList = new ExpressionList(new ArrayList<>(values.length));
			for (String value : values) {
				expressionList.getExpressions().add(new StringValue(value));
			}
			InExpression inExpression = new InExpression(column, expressionList);
			newExpression = orginExpression == null ? inExpression : new AndExpression(orginExpression,inExpression);
		}
		
		return newExpression;
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
