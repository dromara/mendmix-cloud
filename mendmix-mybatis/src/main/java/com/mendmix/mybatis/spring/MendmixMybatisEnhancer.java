/*
 * Copyright 2016-2018 www.mendmix.com.
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

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.mybatis.MybatisConfigs;
import com.mendmix.mybatis.crud.GeneralSqlGenerator;
import com.mendmix.mybatis.metadata.MapperMetadata;
import com.mendmix.mybatis.parser.MybatisMapperParser;
import com.mendmix.mybatis.plugin.JeesuiteMybatisInterceptor;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class MendmixMybatisEnhancer {

	private static final Logger logger = LoggerFactory.getLogger(MendmixMybatisEnhancer.class);

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
			List<MapperMetadata> mappers = MybatisMapperParser.getMapperMetadatas(group);
			for (MapperMetadata mapper : mappers) {
				method.invoke(helper, mapper.getMapperClass());
			}

			method = helperClazz.getDeclaredMethod("processConfiguration", Configuration.class);
			method.invoke(helper, configuration);
		} catch (ClassNotFoundException e) {
			new GeneralSqlGenerator(group, configuration).generate();
		}catch (Exception e) {
			throw e;
		}

		// pageHelper
		try {
			Class<?> pageHelperClazz = Class.forName("com.github.pagehelper.PageInterceptor");
			Interceptor pageInterceptor = (Interceptor) pageHelperClazz.newInstance();
			configuration.addInterceptor(pageInterceptor);
		} catch (Exception e) {
		}
		
		// 注册拦截器
		String[] hanlderNames = MybatisConfigs.getHandlerNames(group);
		JeesuiteMybatisInterceptor interceptor = new JeesuiteMybatisInterceptor(group, hanlderNames);
		configuration.addInterceptor(interceptor);
		interceptor.afterRegister();

		logger.info(">> JeesuiteMybatisEnhancer finshed -> group:{},hanlderNames:{}", group, hanlderNames);

	}
}
