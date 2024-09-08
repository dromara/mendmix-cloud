/*
 * Copyright 2016-2020 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.mybatis.spring;

import java.lang.reflect.Method;
import java.util.List;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.mybatis.MybatisConfigs;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.datasource.UsingDataSource;
import org.dromara.mendmix.mybatis.plugin.operProtect.annotation.SensitiveOperProtect;
import org.dromara.mendmix.mybatis.plugin.rewrite.annotation.DataPermission;
import org.dromara.mendmix.mybatis.plugin.rewrite.annotation.DatapermissionIgnore;
import org.dromara.mendmix.mybatis.plugin.rewrite.annotation.SofeDeleteIgnore;
import org.dromara.mendmix.mybatis.plugin.rewrite.annotation.SqlRewriteIgnore;
import org.dromara.mendmix.mybatis.plugin.rewrite.annotation.TenantRouteIgnore;
import org.dromara.mendmix.mybatis.plugin.rwseparate.UseMaster;
import org.dromara.mendmix.spring.InterceptorHanlder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * 
 * <br>
 * Class Name   : MyBatisInterceptorHanlder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Oct 31, 2020
 */
public class MyBatisInterceptorHandler implements InterceptorHanlder {
	
	private List<String> ignoreTenantUserTypes = ResourceUtils.getList(MybatisConfigs.TENANT_IGNORE_USER_TYPE);

	@Override
	public void preHandler(Method method, Object[] args) {
		//多个方法层级调用 ，以最外层方法定义为准
		if(!MybatisRuntimeContext.isTransactionalOn() && method.isAnnotationPresent(Transactional.class)) {
			boolean readOnly = method.getAnnotation(Transactional.class).readOnly();
			MybatisRuntimeContext.setTransactionalMode(!readOnly);
		}
		
		if(method.isAnnotationPresent(UseMaster.class)){				
			MybatisRuntimeContext.forceUseMaster();
		}
		
		if(method.isAnnotationPresent(TenantRouteIgnore.class)){	
			MybatisRuntimeContext.setIgnoreTenantMode();
		}else if(!MybatisRuntimeContext.isIgnoreTenantMode()){
			//忽略多租户
			AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
			if(currentUser != null 
					&& (ignoreTenantUserTypes.contains(currentUser.getType()))) {
				MybatisRuntimeContext.setIgnoreTenantMode();
			}
			
		}
		//
		if(method.isAnnotationPresent(DataPermission.class)){	
			MybatisRuntimeContext.setDataPermissionStrategy(method.getAnnotation(DataPermission.class));
		}
		
		if(method.isAnnotationPresent(SqlRewriteIgnore.class)){	
			MybatisRuntimeContext.setIgnoreSqlRewrite(true);
		}
		
		if(method.isAnnotationPresent(DatapermissionIgnore.class)){	
			MybatisRuntimeContext.setIgnoreDataPermission(true);
		}
		
		if(method.isAnnotationPresent(SofeDeleteIgnore.class)){	
			MybatisRuntimeContext.setIgnoreSoftDeleteConditon(true);
		}
		
		if(method.isAnnotationPresent(UsingDataSource.class)){
			UsingDataSource annotation = method.getAnnotation(UsingDataSource.class);
			MybatisRuntimeContext.setDatasourceGroup(annotation.group());
			if(annotation.dataSourceKey().length > 0) {
				MybatisRuntimeContext.setTenantDataSourceKey(annotation.dataSourceKey()[0]);
			}
		}
		
		if(method.isAnnotationPresent(SensitiveOperProtect.class)){
			SensitiveOperProtect annotation = method.getAnnotation(SensitiveOperProtect.class);
			MybatisRuntimeContext.setIgnoreOperProtect(annotation.value());
		}
		
	}

	@Override
	public void postHandler(Object result,Exception ex) {
		
	}

	@Override
	public int priority() {
		return 0;
	}

}
