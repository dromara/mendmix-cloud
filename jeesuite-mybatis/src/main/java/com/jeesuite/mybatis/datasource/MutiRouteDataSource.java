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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
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
import com.jeesuite.mybatis.MybatisConfigs;
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
	@Autowired(required = false)
	private DataSourceConfigLoader dataSourceConfigLoader;

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
			dataSourceType = DataSourceType.valueOf(ResourceUtils.getProperty("db.dataSourceType", DataSourceType.Druid.name()));
		} catch (Exception e) {
			throw new IllegalArgumentException("Property 'db.dataSourceType' expect:" + Arrays.toString(DataSourceType.values()));
		}
		
		boolean tenantMode = MybatisConfigs.isTenantModeEnabled();
		 // 属性文件  
        Map<String, Properties> map = new HashMap<String,Properties>(); 
        if(dataSourceConfigLoader != null){
        	List<DataSourceConfig> configs = dataSourceConfigLoader.load();
        	int slaveIndex = 0;
        	for (DataSourceConfig config : configs) {
        		if(!config.isMaster()){
        			config.setIndex(++slaveIndex);
        		}
        		buildDataSourceProperties(tenantMode,config,map);
			}
        }else{
        	if(tenantMode){
        		Properties properties = ResourceUtils.getAllProperties("tenant\\[.*\\]\\.master.*", false);
        		if(properties.isEmpty())throw new RuntimeException("tenant support Db config Like tenant[xxx].master.db.xxx");
        		
        		List<String> tenantIds = properties.keySet().stream().map(s -> {
        			return s.toString().split("\\[|\\]")[1];
        		}).distinct().collect(Collectors.toList());
        		
        		for (String tenantId : tenantIds) {
        			parseDataSourceConfig(tenantId,map);
        			logger.info("Load tenant config finish, -> tenantId:{}",tenantId);
    			}
        	}else{    		
        		parseDataSourceConfig(null,map);
        	}
        }
		
		if(map.isEmpty())throw new RuntimeException("Db config Not found..");
		registerDataSources(tenantMode,map);
		
		if (this.targetDataSources == null || targetDataSources.isEmpty()) {
			throw new IllegalArgumentException("Property 'targetDataSources' is required");
		}
		
		if (this.defaultDataSource == null && !tenantMode) {
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
		Object lookupKey =MultiDataSourceManager.get().getDataSourceKey();
		DataSource dataSource = targetDataSources.get(lookupKey);
		if (dataSource == null) {
			throw new IllegalStateException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
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
	 * @param mapCustom
	 * @param isLatestGroup
	 */
    private void registerDataSources(boolean tenantMode,Map<String, Properties> mapCustom) {  
    	
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

			String beanName = dsKey.replaceAll("\\[|\\]|\\.", "") + "DataSource";
			acf.registerBeanDefinition(beanName, beanDefinitionBuilder.getRawBeanDefinition());

			DataSource ds = (DataSource) this.context.getBean(beanName);
			targetDataSources.put(dsKey, ds);
			//  设置默认数据源
			if (dsKey.equals(MASTER_KEY)) {
				defaultDataSource = ds;
			}
			logger.info("bean[" + dsKey + "] has initialized! lookupKey:" + dsKey);
			//
			MultiDataSourceManager.get().registerDataSourceKey(dsKey);
		} 
        
        addTargetDataSources(targetDataSources);
    }  
	
	/** 
     * 功能说明：解析配置，得到数据源Map 
     * @return 
     * @throws IOException 
     */  
    private void parseDataSourceConfig(String tenantId,Map<String, Properties> mapDataSource){  
    	String tenantPrefix = tenantId == null ? "" : "tenant["+tenantId+"].";
		String datasourceKey = tenantPrefix + MASTER_KEY;
		parseEachConfig(datasourceKey,mapDataSource); 
		
		//解析同组下面的slave
		int index = 1;
		while(true){
			datasourceKey = tenantPrefix + "slave" + index;
			if(!ResourceUtils.containsProperty(datasourceKey + ".db.url") && !ResourceUtils.containsProperty(datasourceKey + ".db.jdbcUrl"))break;
			parseEachConfig(datasourceKey,mapDataSource); 
			index++;
		}
    }  
    
    
    private void parseEachConfig(String keyPrefix,Map<String, Properties> mapDataSource){
    	
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
    	
    	mapDataSource.put(keyPrefix, properties);
    }
    
  
	private void buildDataSourceProperties(boolean tenantMode,DataSourceConfig config, Map<String, Properties> map) {
		if(tenantMode && StringUtils.isBlank(config.getTenantId())){
			throw new IllegalArgumentException("On tenantMode tenantId required ");
		}
		String dsKey = config.isMaster() ? MASTER_KEY : "slave" + config.getIndex();
		if(tenantMode)dsKey = "tenant["+config.getTenantId()+"]." + dsKey;
		Properties properties = new Properties();
		properties.setProperty("url", config.getUrl());
		properties.setProperty("username", config.getUsername());
		properties.setProperty("password", config.getPassword());
		
		String prefix = "db.";
    	Properties tmpProps = ResourceUtils.getAllProperties(prefix);
    	properties.putAll(tmpProps);
		
		map.put(dsKey, properties);
	}
    
} 
