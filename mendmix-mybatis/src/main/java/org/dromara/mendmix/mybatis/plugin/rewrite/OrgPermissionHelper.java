package org.dromara.mendmix.mybatis.plugin.rewrite;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.mybatis.MybatisConfigs;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.spring.InstanceFactory;

/**
 * 
 * <br>
 * @author jiangwei
 * @version 1.0.0
 * @date 2023年2月18日
 */
public class OrgPermissionHelper {
	
	protected static final String DEFAULT_POSITION_GROUP_NAME = "position";
	protected static final String CONTEXT_CURRENT_POSTION_ID = "_ctx_cur_postion_id";
	private static OrganizationProvider organizationProvider;
	
	private static OrganizationProvider getOrganizationProvider() {
		if(organizationProvider != null)return organizationProvider;
		synchronized (OrgPermissionHelper.class) {
			organizationProvider = InstanceFactory.getInstance(OrganizationProvider.class);
		}
		return organizationProvider;
	}


	public static void prepareOrganizationPermission(OnceContextVal invocation
			,Map<String, String[]> dataPermValues
			,String deptPropName){
		String[] values = dataPermValues.get(MybatisConfigs.ORG_DATA_PERM_NAME);
		if(values == null)return;
		if(values.length == 0) {
			dataPermValues.put(deptPropName, values);
			return;
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
			}else{
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
			departmentId = getOrganizationProvider().getPositionOwnDepartmentId(positionId);
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
		return getOrganizationProvider().findSubDepartments(deptId);
	}
}
