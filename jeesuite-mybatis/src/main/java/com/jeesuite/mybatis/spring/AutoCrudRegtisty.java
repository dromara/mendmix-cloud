/*
 * Copyright 2016-2018 www.jeesuite.com.
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
package com.jeesuite.mybatis.spring;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.ibatis.session.Configuration;

import com.jeesuite.mybatis.Configs;
import com.jeesuite.mybatis.crud.GeneralSqlGenerator;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class AutoCrudRegtisty {

	public static void register(Configuration configuration){
		if("default".equals(Configs.getCrudDriver())){
			new GeneralSqlGenerator(configuration).generate();
		}else if("mapper3".equals(Configs.getCrudDriver())){
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
				List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos();
				for (EntityInfo entityInfo : entityInfos) {
					method.invoke(helper, entityInfo.getMapperClass());
				}
				
				method = helperClazz.getDeclaredMethod("processConfiguration", Configuration.class);
				method.invoke(helper,configuration);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}else{
			new GeneralSqlGenerator(configuration).generate();
		}
	}
}
