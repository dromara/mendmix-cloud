/**
 * 
 */
package com.jeesuite.mybatis.datasource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * 自动路由多数据源（读写分离 and 水平分库路由）
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年11月18日
 * @Copyright (c) 2015, jwww
 */
public class MutiRouteDataSource extends AbstractDataSource implements ApplicationContextAware,InitializingBean,EnvironmentAware{  

	private static final Logger logger = LoggerFactory.getLogger(MutiRouteDataSource.class);
	
	private static final String MASTER_KEY = "master";
	
	private ApplicationContext context;
	
	private Map<Object, DataSource> targetDataSources;
	
	private DataSource defaultDataSource;
	
	private int dbGroupNums = 1;//数据库分库组数
	
	//
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
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}  

	@Override
	public void afterPropertiesSet() {
		Map<String, DataSourceInfo> map = parseDataSourceConfFromProperties();
		
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
	}


	/**
	 * 功能说明：根据DataSource创建bean并注册到容器中
	 * @param mapCustom
	 * @param isLatestGroup
	 */
    private void registerDataSources(Map<String, DataSourceInfo> mapCustom) {  
    	
        DefaultListableBeanFactory acf = (DefaultListableBeanFactory) this.context.getAutowireCapableBeanFactory();  
        Iterator<String> iter = mapCustom.keySet().iterator();  
        
       Map<Object, DataSource> targetDataSources = new HashMap<>();
       
        while(iter.hasNext()){  
        	String dsKey = iter.next();  //
        	DataSourceInfo dataSourceInfo = mapCustom.get(dsKey);
        	//如果当前库为最新一组数据库，注册beanName为master
        	
			logger.info(">>>>>begin to initialize datasource："+dsKey + "\n================\n" + dataSourceInfo.toString() + "\n==============");
			
        	BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(DruidDataSource.class);
        	beanDefinitionBuilder.addPropertyValue("driverClassName", dataSourceInfo.driveClassName);
        	beanDefinitionBuilder.addPropertyValue("url", dataSourceInfo.connUrl);
        	beanDefinitionBuilder.addPropertyValue("username", dataSourceInfo.userName);
        	beanDefinitionBuilder.addPropertyValue("password", dataSourceInfo.password);
        	//
        	beanDefinitionBuilder.addPropertyValue("testWhileIdle", true);
        	beanDefinitionBuilder.addPropertyValue("validationQuery", "SELECT 'x'");
        	
        	if(dataSourceInfo.initialSize > 0){
        		beanDefinitionBuilder.addPropertyValue("initialSize", dataSourceInfo.initialSize);
        	}
        	if(dataSourceInfo.maxActive > 0){
        		beanDefinitionBuilder.addPropertyValue("maxActive", dataSourceInfo.maxActive);
        	}
        	if(dataSourceInfo.maxIdle > 0){
        		beanDefinitionBuilder.addPropertyValue("maxIdle", dataSourceInfo.maxIdle);
        	}
        	if(dataSourceInfo.minIdle > 0){
        		beanDefinitionBuilder.addPropertyValue("minIdle", dataSourceInfo.minIdle);
        	}
        	
        	if(dataSourceInfo.maxWait > 0){
        		beanDefinitionBuilder.addPropertyValue("maxWait", dataSourceInfo.maxWait);
        	}
        	
        	if(dataSourceInfo.minEvictableIdleTimeMillis > 0){
        		beanDefinitionBuilder.addPropertyValue("minEvictableIdleTimeMillis", dataSourceInfo.minEvictableIdleTimeMillis);
        	}
        	
        	if(dataSourceInfo.timeBetweenEvictionRunsMillis > 0){
        		beanDefinitionBuilder.addPropertyValue("timeBetweenEvictionRunsMillis", dataSourceInfo.timeBetweenEvictionRunsMillis);
        	}

        	if(dataSourceInfo.maxWait > 0){
        		beanDefinitionBuilder.addPropertyValue("maxWait", dataSourceInfo.maxWait);
        	}
        	
        	beanDefinitionBuilder.addPropertyValue("testOnBorrow", dataSourceInfo.testOnBorrow);
        	beanDefinitionBuilder.addPropertyValue("testOnReturn", dataSourceInfo.testOnReturn);
            
            acf.registerBeanDefinition(dsKey, beanDefinitionBuilder.getRawBeanDefinition());

            DruidDataSource ds = (DruidDataSource)this.context.getBean(dsKey);
            
			targetDataSources.put(dsKey, ds);
			
			// 设置默认数据源
			if(dataSourceInfo.dbGroupIndex == dbGroupNums - 1){				
				defaultDataSource = ds;
			}
            logger.info("bean["+dsKey+"] has initialized! lookupKey:"+dsKey);
            
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
    private Map<String, DataSourceInfo> parseDataSourceConfFromProperties(){  
        // 属性文件  
        Map<String, DataSourceInfo> mapDataSource = new HashMap<String,DataSourceInfo>(); 
        
        dbGroupNums = Integer.parseInt(environment.getProperty("db.group.size", "1"));
        logger.info(">>>>>>dbGroupNums:" + dbGroupNums);
        for (int i = 0; i < dbGroupNums; i++) {
			String groupPrefix = i == 0 ? "" : "group" + i ;
			String datasourceKey = (StringUtils.isNotBlank(groupPrefix) ? groupPrefix + "." : "") + MASTER_KEY;
			DataSourceInfo sourceInfo = new DataSourceInfo(i,datasourceKey); 
			mapDataSource.put(datasourceKey, sourceInfo);
			
			//解析同组下面的slave
			int index = 1;
			wl:while(true){
				datasourceKey = (StringUtils.isNotBlank(groupPrefix) ? groupPrefix + "." : "") + "slave" + index;
				if(!environment.containsProperty(datasourceKey + ".db.url"))break wl;
				sourceInfo = new DataSourceInfo(i,datasourceKey); 
				mapDataSource.put(datasourceKey, sourceInfo);
				index++;
			}
		}
        return mapDataSource;  
    }  
	
	private class DataSourceInfo{  
		//分库ID
		public int dbGroupIndex;
		public String driveClassName;
        public String connUrl;  
        public String userName;  
        public String password;
        public boolean master;
        protected int initialSize;
        protected int maxActive;
        protected int minIdle;
        protected int maxIdle;
        //获取连接等待超时的时间
        protected long maxWait;
        //一个连接在池中最小生存的时间，单位是毫秒
        public long minEvictableIdleTimeMillis;
        //多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
        public long timeBetweenEvictionRunsMillis;
        public boolean testOnBorrow;
        public boolean testOnReturn;
          
		//根据全局配置构造方法		
		public DataSourceInfo(int groupIndex,String keyPrefix) {
			String tmpVal = null;
			this.dbGroupIndex = groupIndex;
			//全局配置
			this.driveClassName = environment.getProperty("db.driverClass");
			this.initialSize = Integer.parseInt(environment.getProperty("db.initialSize","1"));
			this.minIdle = Integer.parseInt(environment.getProperty("db.minIdle","1"));
			this.maxActive = Integer.parseInt(environment.getProperty("db.maxActive","10"));
			this.maxWait = Integer.parseInt(environment.getProperty("db.maxWait","60000"));
			this.minEvictableIdleTimeMillis = Integer.parseInt(environment.getProperty("db.minEvictableIdleTimeMillis","300000"));
			this.timeBetweenEvictionRunsMillis = Integer.parseInt(environment.getProperty("db.timeBetweenEvictionRunsMillis","60000"));
			this.testOnBorrow = Boolean.parseBoolean(environment.getProperty("db.testOnBorrow","false"));
			this.testOnReturn = Boolean.parseBoolean(environment.getProperty("db.testOnReturn","false"));
			
			//私有配置
			this.master = keyPrefix.contains(MASTER_KEY);
			this.connUrl = environment.getProperty(keyPrefix + ".db.url");
			Validate.notBlank(this.connUrl, "Config [%s.db.url] is required", keyPrefix);
			
			this.userName = environment.getProperty(keyPrefix + ".db.username");
			Validate.notBlank(this.userName, "Config [%s.db.username] is required", keyPrefix);
			
			this.password = environment.getProperty(keyPrefix + ".db.password");
			Validate.notBlank(this.password, "Config [%s.db.password] is required", keyPrefix);
			//覆盖全局配置
			if((tmpVal = environment.getProperty(keyPrefix + ".db.initialSize")) != null){				
				this.initialSize = Integer.parseInt(tmpVal);
			}
			if((tmpVal = environment.getProperty(keyPrefix + ".db.minIdle")) != null){				
				this.minIdle = Integer.parseInt(tmpVal);
			}
			if((tmpVal = environment.getProperty(keyPrefix + ".db.maxActive")) != null){				
				this.maxActive = Integer.parseInt(tmpVal);
			}
			if((tmpVal = environment.getProperty(keyPrefix + ".db.minEvictableIdleTimeMillis")) != null){				
				this.minEvictableIdleTimeMillis = Integer.parseInt(tmpVal);
			}
			
			if((tmpVal = environment.getProperty(keyPrefix + ".db.minEvictableIdleTimeMillis")) != null){				
				this.minEvictableIdleTimeMillis = Integer.parseInt(tmpVal);
			}
			
			if((tmpVal = environment.getProperty(keyPrefix + ".db.timeBetweenEvictionRunsMillis")) != null){				
				this.timeBetweenEvictionRunsMillis = Integer.parseInt(tmpVal);
			}
			
			if((tmpVal = environment.getProperty(keyPrefix + ".db.testOnBorrow")) != null){				
				this.testOnBorrow = Boolean.parseBoolean(tmpVal);
			}
			
			if((tmpVal = environment.getProperty(keyPrefix + ".db.testOnReturn")) != null){				
				this.testOnReturn = Boolean.parseBoolean(tmpVal);
			}
			
		}

		@Override
		public String toString() {
			StringBuffer str = new StringBuffer();
			str.append("dbGroupIndex").append(" = ").append(dbGroupIndex).append("\n");
			str.append("role").append(" = ").append(master ? "master" : "slave").append("\n");
			str.append("driveClassName").append(" = ").append(driveClassName).append("\n");
			str.append("connUrl").append(" = ").append(connUrl).append("\n");
			str.append("userName").append(" = ").append(userName);
			return str.toString();
		}    
    }

	
} 
