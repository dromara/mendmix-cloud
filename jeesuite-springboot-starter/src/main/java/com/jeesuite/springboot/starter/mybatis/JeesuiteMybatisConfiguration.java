package com.jeesuite.springboot.starter.mybatis;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.jeesuite.mybatis.datasource.MutiRouteDataSource;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;
import com.jeesuite.mybatis.plugin.PluginConfig;
import com.jeesuite.mybatis.plugin.cache.CacheHandler;
import com.jeesuite.mybatis.plugin.pagination.PaginationHandler;
import com.jeesuite.mybatis.plugin.rwseparate.RwRouteHandler;

import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.MapperHelper;

@Configuration
@EnableConfigurationProperties(MybatisPluginProperties.class)
@ConditionalOnClass(MutiRouteDataSource.class)
@AutoConfigureAfter(MybatisAutoConfiguration.class)
public class JeesuiteMybatisConfiguration implements InitializingBean {

	@Autowired
	private SqlSessionFactory sqlSessionFactory;
	@Autowired
	private MybatisPluginProperties properties;

	@Value("${mybatis.mapper-locations}")
	private String mapperLocations;

	@Override
	public void afterPropertiesSet() throws Exception {
		//

		List<String> hanlders = new ArrayList<>();
	    
		if(org.apache.commons.lang3.StringUtils.isNotBlank(properties.getInterceptorHandlerClass())){
			String[] customHanlderClass = StringUtils.tokenizeToStringArray(properties.getInterceptorHandlerClass(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
			hanlders.addAll(Arrays.asList(customHanlderClass));
		}
		
		if (properties.isCacheEnabled()) {
			hanlders.add(CacheHandler.NAME);
		}

		if (properties.isRwRouteEnabled()) {
			hanlders.add(RwRouteHandler.NAME);
		}
		
		if (properties.isPaginationEnabled()) {
			hanlders.add(PaginationHandler.NAME);
		}

		if (!hanlders.isEmpty()) {
			JeesuiteMybatisInterceptor interceptor = new JeesuiteMybatisInterceptor();
			interceptor.setMapperLocations(mapperLocations);
			interceptor.setInterceptorHandlers(org.apache.commons.lang3.StringUtils.join(hanlders.toArray(), ","));
			sqlSessionFactory.getConfiguration().addInterceptor(interceptor);
			
			Properties p = new Properties();
			p.setProperty(PluginConfig.CRUD_DRIVER, properties.getCrudDriver());
			p.setProperty(PluginConfig.DB_TYPE, properties.getDbType());
			p.setProperty(PluginConfig.CACHE_NULL_VALUE, String.valueOf(properties.isNullValueCache()));
			p.setProperty(PluginConfig.CACHE_EXPIRE_SECONDS, String.valueOf(properties.getCacheExpireSeconds()));
			p.setProperty(PluginConfig.CACHE_DYNAMIC_EXPIRE, String.valueOf(properties.isDynamicExpire()));
			
			interceptor.setProperties(p);
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
