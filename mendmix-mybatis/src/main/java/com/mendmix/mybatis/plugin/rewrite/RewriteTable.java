package com.mendmix.mybatis.plugin.rewrite;

import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.util.JsonUtils;
import com.mendmix.mybatis.plugin.rewrite.annotation.TablePermissionStrategy;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;

/**
 * 
 * <br>
 * @author jiangwei
 * @version 1.0.0
 * @date 2023年4月22日
 */
public class RewriteTable {

	public static String nameDelimiter = "`";
	
	private Table table;
	private String tableName;
	private String rewritedTableName;
	Map<String, String> rewriteColumnMapping;
	private boolean join;
	private boolean appendConditonUsingOn;
	private Object appendConditonTo;
	
	private boolean handleOwner;
	
	private boolean withDeptColumn;
	private boolean usingGroupOrgPerm;
	
	private String[] ownerColumns;
	
	private TablePermissionStrategy tableStrategy;
	
	public RewriteTable(Table table,boolean joinTable) {
		this.table = table;
		tableName = StringUtils.remove(table.getName(), nameDelimiter);
		this.join = joinTable;
	}
	
	public Table getTable() {
		return table;
	}
	public String getTableName() {
		return tableName;
	}
	public String getRewritedTableName() {
		return rewritedTableName;
	}
	public void setRewritedTableName(String rewritedTableName) {
		this.rewritedTableName = rewritedTableName;
	}

	public Map<String, String> getRewriteColumnMapping() {
		return rewriteColumnMapping;
	}
	public void setRewriteColumnMapping(Map<String, String> rewriteColumnMapping) {
		this.rewriteColumnMapping = rewriteColumnMapping;
	}
	
	public boolean isHandleOwner() {
		return handleOwner;
	}

	public void setHandleOwner(boolean handleOwner) {
		this.handleOwner = handleOwner;
	}

	public boolean isJoin() {
		return join;
	}
	public void setJoin(boolean join) {
		this.join = join;
	}
	public boolean isAppendConditonUsingOn() {
		return appendConditonUsingOn;
	}
	public void setAppendConditonUsingOn(boolean appendConditonUsingOn) {
		this.appendConditonUsingOn = appendConditonUsingOn;
	}

	public void setAppendConditonTo(Object appendConditonTo) {
		this.appendConditonTo = appendConditonTo;
	}
	
	public boolean isWithDeptColumn() {
		return withDeptColumn;
	}

	public void setWithDeptColumn(boolean withDeptColumn) {
		this.withDeptColumn = withDeptColumn;
	}

	public boolean isUsingGroupOrgPerm() {
		return usingGroupOrgPerm;
	}

	public void setUsingGroupOrgPerm(boolean usingGroupOrgPerm) {
		this.usingGroupOrgPerm = usingGroupOrgPerm;
	}
	
	public String[] getOwnerColumns() {
		return ownerColumns;
	}

	public void setOwnerColumns(String[] ownerColumns) {
		this.ownerColumns = ownerColumns;
	}

	public TablePermissionStrategy getTableStrategy() {
		return tableStrategy;
	}

	public void setTableStrategy(TablePermissionStrategy tableStrategy) {
		this.tableStrategy = tableStrategy;
	}

	public void updateConditionExpression(Expression expression) {
		if(expression == null)return;
		if(appendConditonUsingOn) {
			LinkedList<Expression> onExpressions = (LinkedList<Expression>) ((Join)appendConditonTo).getOnExpressions();
			onExpressions.set(0, expression);
		}else {
			((PlainSelect)appendConditonTo).setWhere(expression);
		}
	}
	
	public Expression getOriginConditionExpression() {
		if(appendConditonUsingOn) {
			return ((Join)appendConditonTo).getOnExpressions().stream().findFirst().orElse(null);
		}else {
			return ((PlainSelect)appendConditonTo).getWhere();
		}
	}
	
	public boolean containsRewriteField(String field) {
		return rewriteColumnMapping != null && rewriteColumnMapping.containsKey(field);
	}
	
	public boolean hasRewriteColumn() {
		return (rewriteColumnMapping != null && !rewriteColumnMapping.isEmpty()) 
				|| ((ownerColumns != null && ownerColumns.length > 0));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("Table[");
		builder.append(getTableName()).append("]");
		if(rewritedTableName != null) {
			builder.append("\n - rewritedTableName:").append(rewritedTableName);
		}
		builder.append("\n   - handleOwner:").append(handleOwner);
		builder.append("\n   - appendConditonUsingOn:").append(appendConditonUsingOn);
		builder.append("\n   - withDeptColumn:").append(withDeptColumn);
		builder.append("\n   - usingGroupOrgPerm:").append(usingGroupOrgPerm);
		builder.append("\n   - rewriteColumnMapping:").append(rewriteColumnMapping);
		builder.append("\n   - ownerColumns:").append(ownerColumns == null ? null :JsonUtils.toJson(ownerColumns));
		return builder.toString();
	}
	
	
}
