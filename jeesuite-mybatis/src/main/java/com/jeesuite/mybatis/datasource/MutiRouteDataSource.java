/**
 * 
 */
package com.jeesuite.mybatis.datasource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.support.ResourcePropertySource;
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
public class MutiRouteDataSource extends AbstractDataSource implements ApplicationContextAware,InitializingBean{  

	private static final String MASTER_KEY = "master";
	
	private String configLocation = "mysql.properties";

	private ApplicationContext context;
	
	private Map<Object, DataSource> targetDataSources;
	
	private DataSource defaultDataSource;
	
	private int dbGroupNums = 1;//数据库分库组数

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

	public void setConfigLocation(String configLocation) {
		this.configLocation = configLocation;
	}

	@Override
	public void afterPropertiesSet() {
		
		File file = new File(Thread.currentThread().getContextClassLoader().getResource(configLocation).getPath());
		
		if(file == null || !file.exists()){
			throw new RuntimeException("classpath 下无数据库配置文件[默认mysql.properties]或指定configLocation");
		}
		
		Map<String, DataSourceInfo> map = parsePropertiesFile(file);
		
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
        	
			logger.info(" begin to initialize datasource："+dataSourceInfo);
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
     * 功能说明：解析属性文件，得到数据源Map 
     * @return 
     * @throws IOException 
     */  
    private Map<String, DataSourceInfo> parsePropertiesFile(File file){  
        // 属性文件  
        ResourcePropertySource props =  null;  
        logger.info("开始解析数据源文件："+file.getAbsolutePath());
        try {
        	props =  new ResourcePropertySource(file.getName());  
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
          
        Pattern pattern = Pattern.compile("^.*(master|slave\\d?)\\.db.*$");  
          
        Map<String, DataSourceInfo> mapDataSource = new HashMap<String,DataSourceInfo>(); 
        
        int dbGroupIndex = 0;
        // 根据配置文件解析数据源  
        logger.info("==========read db configs begin============");
        for(String keyProp : props.getPropertyNames()) {  
        	Object value = props.getProperty(keyProp);
        	if(!keyProp.trim().endsWith("password"))logger.info(keyProp+ "   =   " + value);
        	if(pattern.matcher(keyProp).matches()){
        		
        		boolean containGroup = keyProp.startsWith("group");
				if(containGroup){
        			dbGroupIndex = Integer.parseInt(keyProp.split("\\.")[0].replace("group", ""));
        			if(dbGroupIndex > dbGroupNums - 1){
        				dbGroupNums = dbGroupIndex + 1;
        			}
        		}
        		String dsName = keyProp.substring(0,keyProp.indexOf(".db")).replace(".", "_"); 
        		DataSourceInfo dsi = null; 
        		if(mapDataSource.containsKey(dsName)){
        			dsi = mapDataSource.get(dsName);
        		}else{
        			dsi = new DataSourceInfo(props); 
        			mapDataSource.put(dsName, dsi);
        			if(containGroup)dsi.dbGroupIndex = dbGroupIndex;
        			if(keyProp.contains(MASTER_KEY)){
        				dsi.master = true;
        			}
        		}
        		if(value == null || StringUtils.isBlank(value.toString()))continue;
				if(keyProp.trim().endsWith("url")){
        			dsi.connUrl = (String)value;  
        		}else if(keyProp.trim().endsWith("username")){
        			dsi.userName = (String)value;  
        		}else if(keyProp.trim().endsWith("password")){
        			dsi.password = (String)value; 
        		}else if(keyProp.trim().endsWith("initialSize")){
        			dsi.initialSize = Integer.parseInt(value.toString()); 
        		}else if(keyProp.trim().endsWith("maxActive")){
        			dsi.maxActive = Integer.parseInt(value.toString()); 
        		}else if(keyProp.trim().endsWith("minIdle")){
        			dsi.minIdle = Integer.parseInt(value.toString()); 
        		}else if(keyProp.trim().endsWith("maxIdle")){
        			dsi.maxIdle = Integer.parseInt(value.toString()); 
        		}else if(keyProp.trim().endsWith("maxWait")){
        			dsi.maxWait = Long.parseLong(value.toString()); 
        		}else if(keyProp.trim().endsWith("minEvictableIdleTimeMillis")){
        			dsi.minEvictableIdleTimeMillis = Long.parseLong(value.toString()); 
        		}else if(keyProp.trim().endsWith("timeBetweenEvictionRunsMillis")){
        			dsi.timeBetweenEvictionRunsMillis = Long.parseLong(value.toString()); 
        		}else if(keyProp.trim().endsWith("testOnBorrow")){
        			dsi.testOnBorrow = Boolean.parseBoolean(value.toString()); 
        		}else if(keyProp.trim().endsWith("testOnReturn")){
        			dsi.testOnReturn = Boolean.parseBoolean(value.toString()); 
        		}
        	} 
        }  
        logger.info("dbGroupNums=" + dbGroupNums);
        logger.info("==========read db configs end============");
        return mapDataSource;  
    }  
    protected static boolean propContains(ResourcePropertySource props ,String key){
		boolean contains = props.containsProperty(key);
		if(contains){
			Object object = props.getProperty(key);
			contains = object != null && !"".equals(object.toString().trim());
		}
		return contains;
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
		public DataSourceInfo(ResourcePropertySource props) {
			this.driveClassName = props.getProperty("db.driverClass").toString();
			if(propContains(props,"db.initialSize")){
				this.initialSize = Integer.parseInt(props.getProperty("db.initialSize").toString());
			}
			if(propContains(props,"db.minIdle")){
				this.minIdle = Integer.parseInt(props.getProperty("db.minIdle").toString());
			}
			if(propContains(props,"db.maxActive")){
				this.maxActive = Integer.parseInt(props.getProperty("db.maxActive").toString());
			}
			if(propContains(props,"db.maxWait")){
				this.maxWait = Long.parseLong(props.getProperty("db.maxWait").toString());
			}
			if(propContains(props,"db.timeBetweenEvictionRunsMillis")){
				this.timeBetweenEvictionRunsMillis = Long.parseLong(props.getProperty("db.timeBetweenEvictionRunsMillis").toString());
			}
			if(propContains(props,"db.minEvictableIdleTimeMillis")){
				this.minEvictableIdleTimeMillis = Long.parseLong(props.getProperty("db.minEvictableIdleTimeMillis").toString());
			}
			if(propContains(props,"db.testOnBorrow")){
				this.testOnBorrow = Boolean.parseBoolean(props.getProperty("db.testOnBorrow").toString());
			}
			if(propContains(props,"db.testOnReturn")){
				this.testOnReturn = Boolean.parseBoolean(props.getProperty("db.testOnReturn").toString());
			}
		}

		@Override
		public String toString() {
			return "DataSourceInfo [dbGroupIndex=" + dbGroupIndex + ", driveClassName=" + driveClassName + ", connUrl="
					+ connUrl + ", userName=" + userName + ", master=" + master + ", initialSize=" + initialSize
					+ ", maxActive=" + maxActive + ", minIdle=" + minIdle + ", maxIdle=" + maxIdle + ", maxWait="
					+ maxWait + ", minEvictableIdleTimeMillis=" + minEvictableIdleTimeMillis
					+ ", timeBetweenEvictionRunsMillis=" + timeBetweenEvictionRunsMillis + ", testOnBorrow="
					+ testOnBorrow + ", testOnReturn=" + testOnReturn + "]";
		}    
    }  
} 
