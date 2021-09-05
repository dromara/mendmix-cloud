package com.jeesuite.mongo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import com.jeesuite.common.util.DateUtils;

/**
 * 
 * <br>
 * Class Name   : BaseObject
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月25日
 */
public abstract class BaseObject {
	
	
	

	@Id
	private String id;
	@NonUpdateable
	private String tenantId;
	private boolean enabled = true;
	private boolean deleted = false;
	@NonUpdateable
	@CreatedBy
	private String createdBy;
	@NonUpdateable
	@CreatedDate
	private Date createdAt;
	@LastModifiedBy
	private String updatedBy;
	@LastModifiedDate
	private Date updatedAt;
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	public String getTenantId() {
		return tenantId;
	}
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}
	/**
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	/**
	 * @return the deleted
	 */
	public boolean isDeleted() {
		return deleted;
	}
	/**
	 * @param deleted the deleted to set
	 */
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	/**
	 * @return the createdBy
	 */
	public String getCreatedBy() {
		return createdBy;
	}
	/**
	 * @param createdBy the createdBy to set
	 */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	/**
	 * @return the createdAt
	 */
	public Date getCreatedAt() {
		return createdAt;
	}
	/**
	 * @param createdAt the createdAt to set
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	/**
	 * @return the updatedBy
	 */
	public String getUpdatedBy() {
		return updatedBy;
	}
	/**
	 * @param updatedBy the updatedBy to set
	 */
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
	/**
	 * @return the updatedAt
	 */
	public Date getUpdatedAt() {
		return updatedAt;
	}
	/**
	 * @param updatedAt the updatedAt to set
	 */
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	public void initOperateLog(boolean create){
		if(create){
			this.createdBy = null;
			this.createdAt = new Date();
			this.updatedBy = this.createdBy;
			this.updatedAt = this.createdAt;
		}else{			
			this.updatedBy = null;
			this.updatedAt = new Date();
		}
	}
	
	public static <T extends BaseObject> void validate(T o){
		if(o == null){
			
		}
	}
	
	public static <T extends BaseObject> boolean isActive(T o){
		if(o == null)return false;
		return o.isEnabled() && !o.isDeleted();
	}
	
	public static void resolveInsertCommonFields(Map<String, Object> datas){
//		if(session != null){			
//			datas.put("createdBy", session.getName());
//			datas.put("updatedBy", session.getName());
//		}
		Date date = new Date();
		datas.put("createdAt", date);
		datas.put("updatedAt", date);
		datas.put("enabled", true);
		datas.put("deleted", false);
	}
	
	public static void resolveUpdateCommonFields(Map<String, Object> datas){
		String usertName = null;
		if(StringUtils.isNotBlank(usertName)) {
			datas.put("updatedBy", usertName);
		}
		datas.put("updatedAt", new Date());
	}
	
	public static String getIdValue(Map<String, Object> datas){
		return getValue(datas, CommonFields.ID_FIELD_NAME_ALIAS, String.class);
	}
	
	public static String getShopIdValue(Map<String, Object> datas){
		return getValue(datas, CommonFields.SHOP_ID_FIELD_NAME, String.class);
	}
	
	public static boolean getBooleanValue(Map<String, Object> datas,String field){
		return getValue(datas, field, Boolean.class);
	}
	
	public static BigDecimal getAmountValue(Map<String, Object> datas,String field){
		return getValue(datas, field, BigDecimal.class).setScale(2, RoundingMode.HALF_UP);
	}
	
	public static String getStringValue(Map<String, Object> datas,String field){
		return getValue(datas, field, String.class);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getValue(Map<String, Object> datas,String field,Class<T> valueClass){
		Object value = datas.get(field);
		if(value != null && value.getClass() == valueClass){
			return (T) value;
		}
		if (valueClass == String.class) {
			value = (value == null) ? null : value.toString();
		}else if (valueClass == Integer.class || valueClass == int.class) {
			value = (value == null) ? 0 : Integer.valueOf(value.toString());
		}else if (valueClass == Boolean.class || valueClass == boolean.class) {
			value = (value == null) ? false : (Boolean.parseBoolean(value.toString()) || "1".equals(value.toString()));
		}else if (value != null && valueClass == Date.class) {
			value = DateUtils.parseDate(value.toString());
		}else if (valueClass == Long.class || valueClass == long.class) {
			value = (value == null) ? 0 : Long.valueOf(value.toString());
		}else if (valueClass == BigDecimal.class) {
            value = (value == null) ? BigDecimal.ZERO : new BigDecimal(value.toString());
        }
		
		return (T) value;
	}
	
	public static boolean isActive(Map<String, Object> datas){
		if(datas == null || datas.isEmpty())return false;
		if(datas.containsKey(CommonFields.DELETED_FIELD_NAME) && (boolean)datas.get(CommonFields.DELETED_FIELD_NAME)){
			return false;
		}
		
		if(datas.containsKey(CommonFields.ENABLED_FIELD_NAME) && !(boolean)datas.get(CommonFields.ENABLED_FIELD_NAME)){
			return false;
		}
		return true;
	}
	
	public static void validate(Map<String, Object> datas){
		if(datas == null || datas.isEmpty()){
			
		}
		
		if(datas.containsKey(CommonFields.DELETED_FIELD_NAME) && (boolean)datas.get(CommonFields.DELETED_FIELD_NAME)){
			
		}
		
		if(datas.containsKey(CommonFields.ENABLED_FIELD_NAME) && !(boolean)datas.get(CommonFields.ENABLED_FIELD_NAME)){
			
		}

		
	}
	
}
