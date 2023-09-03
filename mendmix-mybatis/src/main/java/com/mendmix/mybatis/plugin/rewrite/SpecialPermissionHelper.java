package com.mendmix.mybatis.plugin.rewrite;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.mybatis.MybatisConfigs;
import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.mybatis.plugin.InvocationVals;
import com.mendmix.spring.InstanceFactory;

/**
 * 
 * <br>
 * @author jiangwei
 * @version 1.0.0
 * @date 2023年2月20日
 */
public class SpecialPermissionHelper {
	
	protected static final String DEFAULT_PERM_GROUP_NAME = "positionId";
	protected static final String CONTEXT_CURRENT_PERM_GROUP = "_ctx_cur_perm_group";
	protected static final String CONTEXT_CURRENT_POSTION_ID = "_ctx_cur_postion_id";
	private static OrganizationProvider organizationProvider;
	
	private static OrganizationProvider getOrganizationProvider() {
		if(organizationProvider != null)return organizationProvider;
		synchronized (SpecialPermissionHelper.class) {
			organizationProvider = InstanceFactory.getInstance(OrganizationProvider.class);
		}
		return organizationProvider;
	}


	public static void prepareOrganizationPermission(InvocationVals invocation
			,Map<String, String[]> dataPermValues
			,String deptPropName){
		String[] values = dataPermValues.get(MybatisConfigs.DATA_PERM_SPEC_KEY);
		if(values == null)return;
		if(values.length == 0) {
			dataPermValues.put(deptPropName, values);
			return;
		}
		//
		if(values.length == 1 && SpecialPermType.owner.name().equals(values[0])) {
			return;
		}
		//全部
		for (String value : values) {
			if(SpecialPermType._allValues.name().equals(value)) {
				MybatisRuntimeContext.getSqlRewriteStrategy().setHandleOwner(false);
				return;
			}	
		}
		//
		Set<String> valueList = new HashSet<>(values.length);
		for (String value : values) {
			if(StringUtils.isBlank(value))continue;
			value = value.trim();
			if(SpecialPermType.currentDept.name().equals(value)) {
				if(MybatisConfigs.DATA_PERM_ORG_USING_FULL_CODE_MODE) {
					valueList.add(getCurrentDeptCode());
				}else {
					valueList.add(getCurrentDeptId());
				}
			}else if(SpecialPermType.currentAndSubDept.name().equals(value)) {
				if(MybatisConfigs.DATA_PERM_ORG_USING_FULL_CODE_MODE) {
					valueList.add(getCurrentDeptCode() + "%");
				}else {
					List<String> subDepartmentIds = findSubDepartmentIds(getCurrentDeptId());
				    if(subDepartmentIds != null && !subDepartmentIds.isEmpty()) {
				    	valueList.addAll(subDepartmentIds);
				    }
				}
			}else if(!SpecialPermType.owner.name().equals(value)) {
				valueList.add(value);
			}
		}
		if(!valueList.isEmpty()) {				
			dataPermValues.put(deptPropName, valueList.toArray(new String[0]));
			//更新数据权限数据
			MybatisRuntimeContext.setDataPermissionValues(dataPermValues, false);
		}
	} 
	
	private static String getCurrentDeptId() {
		String departmentId;
		if(!ThreadLocalContext.exists(CONTEXT_CURRENT_POSTION_ID)) {
			departmentId = CurrentRuntimeContext.getAndValidateCurrentUser().getDeptId();
		}else {
			String positionId = ThreadLocalContext.getStringValue(CONTEXT_CURRENT_POSTION_ID);
			departmentId = getOrganizationProvider().positionOwnDepartmentId(positionId);
		}
		if(StringUtils.isBlank(departmentId)) {
			throw new MendmixBaseException("当前登录用户部门ID为空");
		}
		return departmentId;
	}
	
	private static String getCurrentDeptCode() {
		String departmentId = getCurrentDeptId();
		return getOrganizationProvider().deptIdToFullCode(departmentId);
	}
	
	private static List<String> findSubDepartmentIds(String deptId) {
		return getOrganizationProvider().subDepartments(deptId);
	}
}
