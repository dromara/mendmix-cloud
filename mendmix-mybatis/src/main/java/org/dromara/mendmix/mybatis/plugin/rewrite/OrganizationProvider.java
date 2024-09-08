package org.dromara.mendmix.mybatis.plugin.rewrite;

import java.util.List;

/**
 * 
 * <br>
 * @author jiangwei
 * @version 1.0.0
 * @date 2023年2月20日
 */
public interface OrganizationProvider {
		
    String deptIdToFullCode(String deptId);
	
    String getPositionOwnDepartmentId(String positionId);
	
	List<String> findSubDepartments(String deptId);
}
