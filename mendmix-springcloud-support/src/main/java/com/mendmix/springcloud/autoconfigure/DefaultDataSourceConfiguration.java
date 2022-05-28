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
package com.mendmix.springcloud.autoconfigure;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mendmix.mybatis.datasource.DataSourceConfig;
import com.mendmix.mybatis.datasource.MultiRouteDataSource;
import com.mendmix.spring.InstanceFactory;


@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@ConditionalOnClass(name = {"org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"})
@ConditionalOnProperty("master.db.url")
public class DefaultDataSourceConfiguration implements ApplicationContextAware{

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		InstanceFactory.setApplicationContext(context);
	}
	
	@Bean("dataSource")
    public DataSource defaultDataSource(){
    	return new MultiRouteDataSource(DataSourceConfig.DEFAULT_GROUP_NAME);
    }
}
