/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.mybatis.spring;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.mybatis.MybatisConfigs;
import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.mybatis.plugin.rewrite.annotation.DataPermission;
import com.mendmix.mybatis.plugin.rewrite.annotation.RewriteIgnore;
import com.mendmix.mybatis.plugin.rewrite.annotation.SofeDeleteIgnore;
import com.mendmix.mybatis.plugin.rewrite.annotation.TenantIgnore;
import com.mendmix.mybatis.plugin.rwseparate.UseMaster;
import com.mendmix.spring.InterceptorHanlder;

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
public class MyBatisInterceptorHanlder implements InterceptorHanlder {

	private List<String> ignoreTenantUserTypes = ResourceUtils.getList(MybatisConfigs.TENANT_IGNORE_USER_TYPE);

	@Override
	public void preHandler(Method method, Object[] args) {
		//多个方法层级调用 ，以最外层方法定义为准
		if(!MybatisRuntimeContext.isTransactionalOn() && method.isAnnotationPresent(Transactional.class)) {
			MybatisRuntimeContext.setTransactionalMode(true);
		}
		
		if(!MybatisRuntimeContext.isRwRouteAssigned() && method.isAnnotationPresent(UseMaster.class)){				
			MybatisRuntimeContext.userMaster();
		}
		
		if(method.isAnnotationPresent(TenantIgnore.class)){	
			MybatisRuntimeContext.setIgnoreTenant(true);
		}else {
			//忽略多租户
			AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
			if(currentUser != null 
					&& (ignoreTenantUserTypes.contains(currentUser.getType()))) {
				MybatisRuntimeContext.setIgnoreTenant(true);
			}
			
		}
		//
		if(method.isAnnotationPresent(DataPermission.class)){	
			MybatisRuntimeContext.setDataPermissionStrategy(method.getAnnotation(DataPermission.class));
		}
		
		if(method.isAnnotationPresent(RewriteIgnore.class)){	
			MybatisRuntimeContext.setIgnoreSqlRewrite(true);
		}
		
		if(method.isAnnotationPresent(SofeDeleteIgnore.class)){	
			MybatisRuntimeContext.setIgnoreSoftDeleteConditon(true);
		}
	}

	@Override
	public void postHandler(Method method, Object result, Exception ex) {}

	@Override
	public void destory() {
		MybatisRuntimeContext.unsetEveryTime();
	}

}
