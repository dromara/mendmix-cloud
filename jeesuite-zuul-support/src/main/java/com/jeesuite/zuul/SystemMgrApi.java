package com.jeesuite.zuul;

import java.util.List;

import com.jeesuite.common.model.Page;
import com.jeesuite.zuul.model.ApiPermission;
import com.jeesuite.zuul.model.AssignRoleParam;
import com.jeesuite.zuul.model.BizSystem;
import com.jeesuite.zuul.model.Button;
import com.jeesuite.zuul.model.GrantPermParam;
import com.jeesuite.zuul.model.Menu;
import com.jeesuite.zuul.model.Permission;
import com.jeesuite.zuul.model.Role;

/**
 * 
 * 
 * <br>
 * Class Name : SystemMgrApi
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2020-10-19
 */
public interface SystemMgrApi {

	BizSystem getSystemMetadata();

	Integer addRole(Role role);

	void updateRole(Role role);

	void toggleRole(Integer id);

	void deleteRole(Integer id);

	List<Role> listAllRoles();

	Page<Role> pageQueryRoleList(int pageNo, int pageSize, Role example);

	Integer addMenu(Menu Menu);

	void updateMenu(Menu menu);

	void toggleMenu(Integer id);

	void deleteMenu(Integer id);

	Integer addButton(Button Menu);

	void updateButton(Button menu);

	void toggleButton(Integer id);

	void deleteButton(Integer id);

	List<Permission> findAllPermissions();

	List<Permission> findRolePermissions(Integer roleId);

	void grantRolePermissions(GrantPermParam param);

	void assignUserRole(AssignRoleParam param);

	List<ApiPermission> findApiPermission(String appId);

	List<ApiPermission> findUserApiPermissions(String userId);

	List<Menu> findUserMenuPermissions(String userId);

	List<Button> findUserButtonPermissions(String userId);

	List<Role> findUserRoles(String userId);

}
