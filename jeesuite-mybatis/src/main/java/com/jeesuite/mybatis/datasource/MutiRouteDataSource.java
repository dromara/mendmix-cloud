/**
 * 
 */
package com.jeesuite.mybatis.datasource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.mybatis.datasource.builder.DruidDataSourceBuilder;
import com.jeesuite.mybatis.datasource.builder.HikariCPDataSourceBuilder;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;

/**
 * 自动路由多数据源（读写分离）
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年11月18日
 * @Copyright (c) 2015, jwww
 */
public class MutiRouteDataSource extends AbstractDataSource implements ApplicationContextAware,InitializingBean{  

	private static final Logger logger = LoggerFactory.getLogger(MutiRouteDataSource.class);
	
	private static final String MASTER_KEY = "master";
	
	private DataSourceType dataSourceType = DataSourceType.Druid;
	
	private ApplicationContext context;
	
	private Map<Object, DataSource> targetDataSources;
	
	private DataSource defaultDataSource;
	
	@Autowired
	private Environment environment;

	private DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

	public void addTargetDataSources(Map<Object, DataSource> targetDataSources) {
		if(this.targetDataSources == null){			
			this.targetDataSources = targetDataSources;
		}else{
			this.targetDataSources.putAll(targetDataSources);
		}
	}

	public void setDataSourceLookup(DataSourceLookup dataSourceLookup) {
		this.dataSourceLookup = (dataSourceLookup != null ? dataSourceLookup : new JndiDataSourceLookup());
	}


	@Override
	public void afterPropertiesSet() {
		
		try {	
			dataSourceType = DataSourceType.valueOf(ResourceUtils.getProperty("db.DataSourceType", DataSourceType.Druid.name()));
		} catch (Exception e) {
			throw new IllegalArgumentException("Property 'db.DataSourceType' expect:" + Arrays.toString(DataSourceType.values()));
		}
				
		Map<String, Properties> map = parseDataSourceConfig();
		
		if(map.isEmpty())throw new RuntimeException("Db config Not found..");
		registerDataSources(map);
		
		if (this.targetDataSources == null || targetDataSources.isEmpty()) {
			throw new IllegalArgumentException("Property 'targetDataSources' is required");
		}
		
		if (this.defaultDataSource == null) {
			throw new IllegalArgumentException("Property 'defaultDataSource' is required");
		}
	}

	protected Object resolveSpecifiedLookupKey(Object lookupKey) {
		return lookupKey;
	}

	protected DataSource resolveSpecifiedDataSource(Object dataSource) throws IllegalArgumentException {
		if (dataSource instanceof DataSource) {
			return (DataSource) dataSource;
		}
		else if (dataSource instanceof String) {
			return this.dataSourceLookup.getDataSource((String) dataSource);
		}
		else {
			throw new IllegalArgumentException(
					"Illegal data source value - only [javax.sql.DataSource] and String supported: " + dataSource);
		}
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
		Object lookupKey = determineCurrentLookupKey();
		if(lookupKey == null){
			return defaultDataSource;
		}
		DataSource dataSource = targetDataSources.get(lookupKey);
		if (dataSource == null) {
			throw new IllegalStateException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
		}
		return dataSource;
	}


     protected Object determineCurrentLookupKey() {   
        return DataSourceContextHolder.get().getDataSourceKey();  
     }  
     

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
		InstanceFactory.setInstanceProvider(new SpringInstanceProvider(context));
	}


	/**
	 * 功能说明：根据DataSource创建bean并注册到容器中
	 * @param mapCustom
	 * @param isLatestGroup
	 */
    private void registerDataSources(Map<String, Properties> mapCustom) {  
    	
        DefaultListableBeanFactory acf = (DefaultListableBeanFactory) this.context.getAutowireCapableBeanFactory();  
        Iterator<String> iter = mapCustom.keySet().iterator();  
        
       Map<Object, DataSource> targetDataSources = new HashMap<>();
       
       BeanDefinitionBuilder beanDefinitionBuilder = null;
		while (iter.hasNext()) {
			String dsKey = iter.next(); //
			Properties nodeProps = mapCustom.get(dsKey);
			// 如果当前库为最新一组数据库，注册beanName为master
			logger.info(">>>>>begin to initialize datasource：" + dsKey + "\n================\n"
			        + String.format("url:%s,username:%s", nodeProps.getProperty("url"),nodeProps.getProperty("username"))
					+ "\n==============");
			if (DataSourceType.Druid == dataSourceType) {
				beanDefinitionBuilder = DruidDataSourceBuilder.builder(nodeProps);
			} else if (DataSourceType.HikariCP == dataSourceType) {
				beanDefinitionBuilder = HikariCPDataSourceBuilder.builder(nodeProps);
			}

			acf.registerBeanDefinition(dsKey, beanDefinitionBuilder.getRawBeanDefinition());

			DataSource ds = (DataSource) this.context.getBean(dsKey);
			targetDataSources.put(dsKey, ds);
			//  设置默认数据源
			if (dsKey.equals(MASTER_KEY)) {
				defaultDataSource = ds;
			}
			logger.info("bean[" + dsKey + "] has initialized! lookupKey:" + dsKey);
			//
			DataSourceContextHolder.get().registerDataSourceKey(dsKey);
		} 
        
        addTargetDataSources(targetDataSources);
    }  
	
	/** 
     * 功能说明：解析配置，得到数据源Map 
     * @return 
     * @throws IOException 
     */  
    private Map<String, Properties> parseDataSourceConfig(){  
        // 属性文件  
        Map<String, Properties> mapDataSource = new HashMap<String,Properties>(); 
        
		String datasourceKey = MASTER_KEY;
		Properties nodeProperties = parseNodeConfig(datasourceKey); 
		mapDataSource.put(datasourceKey, nodeProperties);
		
		//解析同组下面的slave
		int index = 1;
		while(true){
			datasourceKey = "slave" + index;
			if(!ResourceUtils.containsProperty(datasourceKey + ".db.url") && !ResourceUtils.containsProperty(datasourceKey + ".db.jdbcUrl"))break;
			nodeProperties = parseNodeConfig(datasourceKey); 
			mapDataSource.put(datasourceKey, nodeProperties);
			index++;
		}
		
        return mapDataSource;  
    }  
    
    
    private Properties parseNodeConfig(String keyPrefix){
    	Properties properties = new Properties();
    	String prefix = "db.";
    	Properties tmpProps = ResourceUtils.getAllProperties(prefix);
    	
    	String value;
    	for (Entry<Object, Object> entry : tmpProps.entrySet()) {
    		value = environment.getProperty(entry.getKey().toString());
    		if(value == null)value = entry.getValue().toString();
    		properties.setProperty(entry.getKey().toString().replace(prefix, ""), value);
    	}
    	//
    	prefix = keyPrefix + ".db.";
    	tmpProps = ResourceUtils.getAllProperties(prefix);
    	for (Entry<Object, Object> entry : tmpProps.entrySet()) {
    		value = environment.getProperty(entry.getKey().toString());
    		if(value == null)value = entry.getValue().toString();
    		properties.setProperty(entry.getKey().toString().replace(prefix, ""), value);
    	}
    	return properties;
    }
    
} 
