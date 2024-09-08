package org.dromara.mendmix.mybatis.plugin.datalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.constants.DataChangeType;
import org.dromara.mendmix.common.util.BeanUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年6月2日
 */
@JsonInclude(Include.NON_NULL)
public class DataChangeItem {

	private static List<String> ignoreFields = Arrays.asList("createdAt","updatedAt","updatedBy");
	private static String scopeSystemIdField = "systemId";
	private static String scopeTenantIdField = "tenantId";
	
	private String entityName;
	private String id;
	private String systemId;
	private String tenantId;
	private DataChangeType changeType;
	@JsonIgnore
	private Map<String, Object> oldValue;
	private Map<String, Object> newValue;
	
	private List<ChangeValuePair> changeFields;
	
	public DataChangeItem() {}
	
	
	public DataChangeItem(String entityName,String id,DataChangeType changeType) {
		this.entityName = entityName;
		this.id = id;
		this.changeType = changeType;
	}
	
	public DataChangeItem(String entityName,String id,DataChangeType changeType, Object oldValue) {
		this.entityName = entityName;
		this.id = id;
		this.changeType = changeType;
		if(oldValue != null) {
			this.oldValue = BeanUtils.beanToMap(oldValue,false,true);
			if(this.oldValue.containsKey(scopeSystemIdField)) {
				systemId = this.oldValue.get(scopeSystemIdField).toString();
			}
			if(this.oldValue.containsKey(scopeTenantIdField)) {
				tenantId = this.oldValue.get(scopeTenantIdField).toString();				
			}
		}
	}
	
	public String getEntityName() {
		return entityName;
	}
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getSystemId() {
		return systemId;
	}
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}
	public String getTenantId() {
		return tenantId;
	}
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	public DataChangeType getChangeType() {
		return changeType;
	}
	public void setChangeType(DataChangeType changeType) {
		this.changeType = changeType;
	}

	public Map<String, Object> getOldValue() {
		return oldValue;
	}

	public void setOldValue(Map<String, Object> oldValue) {
		this.oldValue = oldValue;
	}

	public Map<String, Object> getNewValue() {
		return newValue;
	}

	public void setNewValue(Map<String, Object> newValue) {
		this.newValue = newValue;
	}
	
	public List<ChangeValuePair> getChangeFields() {
		return changeFields;
	}

	public void setChangeFields(List<ChangeValuePair> changeFields) {
		this.changeFields = changeFields;
	}


	public void updateNewValue(Object newValue) {
		if(newValue != null) {
			this.newValue = BeanUtils.beanToMap(newValue,false,true);
			if(this.oldValue != null) {
				buildChangeValuePairs();
				this.oldValue = null;
			}else {
				if(this.newValue.containsKey(scopeSystemIdField)) {
					systemId = this.newValue.get(scopeSystemIdField).toString();
				}
				if(this.newValue.containsKey(scopeTenantIdField)) {
					tenantId = this.newValue.get(scopeTenantIdField).toString();				
				}
			}
		}
	}
	
	public ChangeValuePair getChangeFieldValue(String fieldName) {
		if(changeFields == null || changeFields.isEmpty())return null;
		return changeFields.stream().filter(
				o -> fieldName.equals(o.fieldName)
		).findFirst().orElse(null);
	}
	
	public String getNewFieldValue(String fieldName) {
		if(newValue == null || newValue.isEmpty())return null;
		return Objects.toString(newValue.get(fieldName), null);
	}
	
	private void buildChangeValuePairs() {
		Set<String> fields = this.newValue.keySet();
		changeFields = new ArrayList<>();
		String _oldValue;
		String _newValue;
		for (String field : fields) {
			if(ignoreFields.contains(field))continue;
			_oldValue = Objects.toString(this.oldValue.get(field), null);
			_newValue = Objects.toString(this.newValue.get(field), null);
			if(StringUtils.equals(_oldValue, _newValue)) {
				continue;
			}
			changeFields.add(new ChangeValuePair(field, _oldValue, _newValue));
		}
	}

	public static class ChangeValuePair{
		String fieldName;
		String beforeValue;
		String afterValue;
		
		public ChangeValuePair() {}
		
		public ChangeValuePair(String fieldName, String beforeValue, String afterValue) {
			this.fieldName = fieldName;
			this.beforeValue = beforeValue;
			this.afterValue = afterValue;
		}

		public String getFieldName() {
			return fieldName;
		}
		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}
		public String getBeforeValue() {
			return beforeValue;
		}
		public void setBeforeValue(String beforeValue) {
			this.beforeValue = beforeValue;
		}
		public String getAfterValue() {
			return afterValue;
		}
		public void setAfterValue(String afterValue) {
			this.afterValue = afterValue;
		}

		@Override
		public String toString() {
			return "ChangeValuePair [fieldName=" + fieldName + ", beforeValue=" + beforeValue + ", afterValue="
					+ afterValue + "]";
		}
		
	}
	
}
