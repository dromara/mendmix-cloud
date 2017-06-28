package com.jeesuite.springboot.starter.mybatis;

import java.util.List;
import java.util.Properties;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.github.pagehelper.PageInterceptor;
import com.jeesuite.mybatis.datasource.MutiRouteDataSource;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;

import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.MapperHelper;

@Configuration
@EnableConfigurationProperties(PluginProperties.class)
@ConditionalOnClass(MutiRouteDataSource.class)
@AutoConfigureAfter(MybatisAutoConfiguration.class)
public class JeesuiteMybatisConfiguration implements InitializingBean {

	@Autowired
	private SqlSessionFactory sqlSessionFactory;
	@Autowired
	private PluginProperties properties;

	@Value("${mybatis.mapper-locations}")
	private String mapperLocations;

	@Override
	public void afterPropertiesSet() throws Exception {
		//
		try {
			PageInterceptor interceptor = new PageInterceptor();
			Properties properties = new Properties();
			properties.setProperty("dialect", "com.github.pagehelper.PageHelper");
			interceptor.setProperties(properties);
			sqlSessionFactory.getConfiguration().addInterceptor(interceptor);
		} catch (Exception e) {}

		String interceptorHandlers = null;
		if (properties.isCacheEnabled()) {
			interceptorHandlers = "cache";
		}

		if (properties.isRwRouteEnabled()) {
			interceptorHandlers = interceptorHandlers == null ? "rwRoute" : interceptorHandlers + ",rwRoute";
		}

		if (properties.isDbShardEnabled()) {
			interceptorHandlers = interceptorHandlers == null ? "dbShard" : interceptorHandlers + ",dbShard";
		}

		if (interceptorHandlers != null) {
			JeesuiteMybatisInterceptor interceptor = new JeesuiteMybatisInterceptor();
			interceptor.setCrudDriver("mapper3");
			interceptor.setMapperLocations(mapperLocations);
			interceptor.setInterceptorHandlers(interceptorHandlers);
			sqlSessionFactory.getConfiguration().addInterceptor(interceptor);
			interceptor.afterPropertiesSet();
		}else{
			MybatisMapperParser.setMapperLocations(mapperLocations);
		}

		//
		MapperHelper mapperHelper = new MapperHelper();
		Config config = new Config();
		config.setNotEmpty(false);
		mapperHelper.setConfig(config);
		List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos();
		for (EntityInfo entityInfo : entityInfos) {
			mapperHelper.registerMapper(entityInfo.getMapperClass());
		}

		mapperHelper.processConfiguration(sqlSessionFactory.getConfiguration());

	}

}
