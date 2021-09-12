package com.jeesuite.springboot.starter.mybatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;

import com.jeesuite.mybatis.datasource.DataSourceConfig;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.spring.JeesuiteMybatisEnhancer;


@Configuration
@AutoConfigureAfter(MybatisAutoConfiguration.class)
@ConditionalOnClass(MybatisAutoConfiguration.class)
public class DefaultMybatisConfiguration implements InitializingBean{

	@Autowired
	private SqlSessionFactory sqlSessionFactory;
	
	@Value("${mybatis.mapper-locations}")
	private String mapperLocations;

	@Override
	public void afterPropertiesSet() throws Exception {
		Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(new DefaultResourceLoader()).getResources(mapperLocations);
		String group = DataSourceConfig.DEFAULT_GROUP_NAME;
		MybatisMapperParser.addMapperLocations(group,resources);
		JeesuiteMybatisEnhancer.handle(group,sqlSessionFactory.getConfiguration());
	}
}
