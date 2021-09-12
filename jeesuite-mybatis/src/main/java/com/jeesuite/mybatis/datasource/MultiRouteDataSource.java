/**
 * 
 */
package com.jeesuite.mybatis.datasource;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.datasource.AbstractDataSource;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.WebConstants;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.mybatis.MybatisConfigs;
import com.jeesuite.mybatis.MybatisRuntimeContext;
import com.jeesuite.mybatis.datasource.builder.DruidDataSourceBuilder;
import com.jeesuite.mybatis.datasource.builder.HikariCPDataSourceBuilder;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;

/**
 * 自动路由多数据源（读写分离/多租户）
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年11月18日
 * @Copyright (c) 2015, jwww
 */
public class MultiRouteDataSource extends AbstractDataSource implements ApplicationContextAware,InitializingBean{  

	private static final Logger logger = LoggerFactory.getLogger(MultiRouteDataSource.class);

	private DataSourceType dataSourceType = DataSourceType.druid;
	
	private ApplicationContext context;
	
	private Map<String, DataSource> targetDataSources = new HashMap<>();

	private String group;
	private boolean dsKeyWithTenant = false;
	//每个master对应slave数
	private Map<String, Integer> slaveNumsMap = new HashMap<>();

	public MultiRouteDataSource(String group) {
		this.group = group;
		dsKeyWithTenant = MybatisConfigs.isSchameSharddingTenant(group);
	}

	@Override
	public void afterPropertiesSet() {
		List<DataSourceConfig> dsConfigs = DataSoureConfigHolder.getConfigs(group);

		for (DataSourceConfig dataSourceConfig : dsConfigs) {			
			registerRealDataSource(dataSourceConfig);
		}
	
		
		if (this.targetDataSources == null || targetDataSources.isEmpty()) {
			throw new IllegalArgumentException("Property 'targetDataSources' is required");
		}
		
		logger.info("init multiRouteDataSource[{}] finished -> slaveNums:{},dsKeyWithTenant:{}",group,slaveNumsMap,dsKeyWithTenant);
	}

	private String currentDataSourceKey() {
		DataSourceContextVals context = MybatisRuntimeContext.getDataSourceContextVals();
		boolean useMaster = context.master == null ? true : context.master;
		int index = 0;
		String tenantId = dsKeyWithTenant ? context.tenantId : null;
		if(dsKeyWithTenant && StringUtils.isBlank(tenantId)) {
			throw new JeesuiteBaseException("Can't get [tentantId] from currentContext");
		}
		if(!useMaster) {
			if(slaveNumsMap.isEmpty()) {
				useMaster = true;
			}else {
				String subGroup = dsKeyWithTenant ? group + WebConstants.UNDER_LINE + tenantId : group;
				if(!slaveNumsMap.containsKey(subGroup)) {
					useMaster = true;
				}else {
					Integer slaveNums = slaveNumsMap.get(subGroup);
					//
					if(slaveNums > 1) {
						index = RandomUtils.nextInt(0, slaveNums);
					}
				}
			}
		}
		return DataSourceConfig.buildDataSourceKey(group, tenantId, useMaster, index);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return determineTargetDataSource().getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return determineTargetDataSource().getConnection(username, password);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(this)) {
			return (T) this;
		}
		return determineTargetDataSource().unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return (iface.isInstance(this) || determineTargetDataSource().isWrapperFor(iface));
	}

	protected DataSource determineTargetDataSource() {
		String lookupKey = currentDataSourceKey();
		DataSource dataSource = targetDataSources.get(lookupKey);
	    if (dataSource == null) {
			throw new JeesuiteBaseException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
		}
		return dataSource;
	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
		InstanceFactory.setInstanceProvider(new SpringInstanceProvider(context));
	}


	/**
	 * 功能说明：根据DataSource创建bean并注册到容器中
	 * @param config
	 */
	private void registerRealDataSource(DataSourceConfig config) {
		
		config.validate();
		
		Properties props = convertProperties(config);

		DefaultListableBeanFactory acf = (DefaultListableBeanFactory) this.context.getAutowireCapableBeanFactory();

		BeanDefinitionBuilder beanDefinitionBuilder = null;
		if (DataSourceType.druid == dataSourceType) {
			beanDefinitionBuilder = DruidDataSourceBuilder.builder(props);
		} else if (DataSourceType.hikariCP == dataSourceType) {
			beanDefinitionBuilder = HikariCPDataSourceBuilder.builder(props);
		}

		String dsKey = config.dataSourceKey();
		String beanName = config.getGroup() + "DataSource";
		acf.registerBeanDefinition(beanName, beanDefinitionBuilder.getRawBeanDefinition());

		DataSource ds = (DataSource) this.context.getBean(beanName);
		targetDataSources.put(dsKey, ds);
		//保存slave节点数
		if(dsKey.contains(DataSourceConfig.SLAVE_KEY)) {
			String subGroup = StringUtils.splitByWholeSeparator(dsKey, "_slave")[0];
			if(slaveNumsMap.containsKey(subGroup)) {
				slaveNumsMap.put(subGroup, slaveNumsMap.get(subGroup) + 1);
			}else {
				slaveNumsMap.put(subGroup, 1);
			}
		}
		
		
		logger.info(">>register realDataSource[{}] finished! -> config:{}",config.dataSourceKey(),config.toString());

	}

	private Properties convertProperties(DataSourceConfig config) {
		
		Properties properties = new Properties();
		Field[] fields = config.getClass().getDeclaredFields();
		String value;
		for (Field field : fields) {
			if("class".equals(field.getName()))continue;
			field.setAccessible(true);
			try {
				value = field.get(config).toString();
			} catch (Exception e) {
				value = ResourceUtils.getProperty("db." + field.getName());
			}
			
			if(value != null) {
				properties.setProperty(field.getName(), value);
			}
		}
		return properties;
	}  

    
} 
