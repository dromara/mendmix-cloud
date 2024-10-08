package org.dromara.mendmix.mybatis.plugin.rewrite;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.mybatis.plugin.rewrite.annotation.DataPermissionItem;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;

/**
 * 
 * <br>
 * @author jiangwei
 * @version 1.0.0
 * @date 2023年4月25日
 */
public class RewriteTable {

	public static String nameDelimiter = "`";
	
	private Table table;
	private String tableName;
	private String rewritedTableName;
	Map<String, List<String>> rewriteColumnMapping;
	private boolean join;
	private boolean appendConditonUsingOn;
	private Object appendConditonTo;
	
	private boolean handleOwner;
	
	private boolean withDeptColumn;
	private boolean usingGlobalOrgPerm;
	
	private String[] ownerColumns;
	
	private boolean ownerUsingAnd;
	
	private DataPermissionItem tableStrategy;
	
	private Map<String,Expression> groupExpressions;
	
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

	public Map<String, List<String>> getRewriteColumnMapping() {
		return rewriteColumnMapping;
	}
	public void setRewriteColumnMapping(Map<String, List<String>> rewriteColumnMapping) {
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

	public boolean isUsingGlobalOrgPerm() {
		return usingGlobalOrgPerm;
	}

	public void setUsingGlobalOrgPerm(boolean usingGlobalOrgPerm) {
		this.usingGlobalOrgPerm = usingGlobalOrgPerm;
	}
	
	public String[] getOwnerColumns() {
		return ownerColumns;
	}

	public void setOwnerColumns(String[] ownerColumns) {
		this.ownerColumns = ownerColumns;
	}
	
	public boolean isOwnerUsingAnd() {
		return ownerUsingAnd;
	}

	public void setOwnerUsingAnd(boolean ownerUsingAnd) {
		this.ownerUsingAnd = ownerUsingAnd;
	}

	public DataPermissionItem getTableStrategy() {
		return tableStrategy;
	}

	public void setTableStrategy(DataPermissionItem tableStrategy) {
		this.tableStrategy = tableStrategy;
	}
	
	public Collection<Expression> getGroupExpressions() {
		return groupExpressions == null ? null : groupExpressions.values();
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
	
	public void addGroupExpressions(Expression expression) {
		if(expression == null)return;
		if(groupExpressions == null) {
			groupExpressions = new HashMap<>(3);
		}
		//忽略相同的条件
		String key = expression.toString();
		if(!groupExpressions.containsKey(key)) {
			groupExpressions.put(key, expression);
		}
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
		builder.append("\n   - usingGroupOrgPerm:").append(usingGlobalOrgPerm);
		builder.append("\n   - rewriteColumnMapping:").append(rewriteColumnMapping);
		builder.append("\n   - ownerColumns:").append(ownerColumns == null ? null :JsonUtils.toJson(ownerColumns));
		return builder.toString();
	}
	
	
}
