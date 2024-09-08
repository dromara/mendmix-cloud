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
package org.dromara.mendmix.mybatis.plugin.operProtect;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlCommandType;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.exception.MainErrorType;
import org.dromara.mendmix.mybatis.MybatisConfigs;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.kit.MybatisSqlRewriteUtils;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.dromara.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.PluginInterceptorHandler;
import org.dromara.mendmix.mybatis.plugin.operProtect.annotation.SensitiveOperProtect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月11日
 */
public class SensitiveOperProtectHandler implements PluginInterceptorHandler{

	private static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");
	
	private boolean allScopes;
	
	@Override
	public void start(MendmixMybatisInterceptor context) {
		allScopes = MybatisConfigs.getBoolean(context.getGroupName(), "mybatis.operProtect.allScopes", true);
	}

	@Override
	public void close() {}

	@Override
	public Object onInterceptor(OnceContextVal invocation) throws Throwable {
        if(MybatisRuntimeContext.isIgnoreOperProtect()) {
        	return null;
        }
        if(invocation.getPageObject() != null) {
        	return null;
        }
        
        if(invocation.getMappedStatement().getSqlCommandType().equals(SqlCommandType.INSERT)) {
        	return null;
        }
        
        SensitiveOperProtect annotation = MapperMetadata.getAnnotation(invocation, SensitiveOperProtect.class);
        if(annotation != null) {
        	if(!annotation.value()) {
        		return null;
        	}
        }else if(!allScopes) {
        	return null;
        }

        BoundSql boundSql = invocation.getBoundSql();
        boolean withCondition = !boundSql.getParameterMappings().isEmpty();
        if(!withCondition) {
        	String sql = boundSql.getSql();
        	withCondition = MybatisSqlRewriteUtils.withWhereConditions(sql);
        }
        if(!withCondition) {
        	logger.error("<db_oper_protect> mapper:{},sql:{}",invocation.getMappedStatement().getId(),boundSql.getSql());
        	throw new MendmixBaseException(MainErrorType.DB_OPTER_FORBIDDEN_ERROR);
        }
		return null;
	}

	@Override
	public void onFinished(OnceContextVal invocation, Object result) {
		
	}

	@Override
	public int interceptorOrder() {
		return 1;
	}

}
