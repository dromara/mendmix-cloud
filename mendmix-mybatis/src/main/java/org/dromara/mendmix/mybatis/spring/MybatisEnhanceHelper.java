/*
 * Copyright 2016-2018 dromara.org.
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

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.dromara.mendmix.mybatis.crud.GeneralSqlGenerator;
import org.dromara.mendmix.mybatis.kit.MybatisMapperParser;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.dromara.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * mybatis 增强处理
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class MybatisEnhanceHelper {

	private static final Logger logger = LoggerFactory.getLogger(MybatisEnhanceHelper.class);

	public static void handle(String group, Configuration configuration) throws Exception {
		
		try {
			Class<?> helperClazz = Class.forName("tk.mybatis.mapper.mapperhelper.MapperHelper");
			Object helper = helperClazz.newInstance();
			Class<?> configClazz = Class.forName("tk.mybatis.mapper.entity.Config");
			Object config = configClazz.newInstance();
			Method method = configClazz.getDeclaredMethod("setNotEmpty", boolean.class);
			method.invoke(config, false);
			method = helperClazz.getDeclaredMethod("setConfig", configClazz);
			method.invoke(helper, config);
			
			method = helperClazz.getDeclaredMethod("registerMapper", Class.class);
			List<MapperMetadata> entityInfos = MybatisMapperParser.getMapperMetadatas(group);
			for (MapperMetadata entityInfo : entityInfos) {
				method.invoke(helper, entityInfo.getMapperClass());
			}

			method = helperClazz.getDeclaredMethod("processConfiguration", Configuration.class);
			method.invoke(helper, configuration);
			logger.info(">> register [tk.mybatis.mapper.mapperhelper.MapperHelper] for group:{} finish,mapperNums:{}",group,entityInfos.size());
		} catch (ClassNotFoundException e) {
			new GeneralSqlGenerator(group, configuration).generate();
		}catch (Exception e) {
			throw e;
		}
		//pageHelper
		try {			
			Class<?> pageHelperClazz = Class.forName("com.github.pagehelper.PageInterceptor");
			Interceptor pageInterceptor = (Interceptor) pageHelperClazz.newInstance();
			configuration.addInterceptor(pageInterceptor);
		} catch (Exception e) {}
				
		// 注册默认拦截器
		MendmixMybatisInterceptor interceptor = new MendmixMybatisInterceptor(group);
		configuration.addInterceptor(interceptor);
		interceptor.afterRegister();
		logger.info(">> JeesuiteMybatisEnhancer finshed -> group:{}",group);
		
		List<Interceptor> interceptors = configuration.getInterceptors();
		for (Interceptor inter : interceptors) {
			logger.info(">> Add Mybaits Interceptor:{}",inter.getClass().getName());
		}

	}
}
