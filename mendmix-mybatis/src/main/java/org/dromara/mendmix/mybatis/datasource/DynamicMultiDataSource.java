/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.mybatis.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.mybatis.MybatisConfigs;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.datasource.builder.DruidDataSourceBuilder;
import org.dromara.mendmix.mybatis.datasource.builder.HikariCPDataSourceBuilder;
import org.dromara.mendmix.spring.CommonApplicationEvent;
import org.dromara.mendmix.spring.InstanceFactory;
import org.dromara.mendmix.spring.SpringEventType;
import org.dromara.mendmix.spring.helper.EnvironmentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.AbstractDataSource;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * 自动路由多数据源（读写分离/多租户）
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年11月18日
 * @Copyright (c) 2015, jwww
 */
public class DynamicMultiDataSource extends AbstractDataSource implements InitializingBean,DisposableBean {  

	private static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");
	
	//所有数据源租户标识
	private static Set<String> allTenantDataSourceKeys = new HashSet<>();

	private DataSourceType dataSourceType = DataSourceType.druid;
	
	private Map<String, DataSourceConfig> targetDataSourceConfigs = new HashMap<>();
	
	private Map<String, DataSource> targetDataSources = new HashMap<>();

	private String group;
	private String mixTenantDataSourceKey; //混合模式字段隔离租户默认数据源
	//库隔离租户模式
	private boolean schemaTenantMode = false;
	//库隔离数据源租户标识
	private Set<String> tenantDataSourceKeys = new HashSet<>();
	//每个master对应slave数
	private Map<String, Integer> slaveNumsMap = new HashMap<>();
	
	private Map<String, String> tenantIdToKeyMapping = new ConcurrentHashMap<>();
	
	private Map<String, Set<String>> dataSourceKeyToTenantMapping = new ConcurrentHashMap<>();
	
	private String missTenantDataSourceKey;
	
	@Autowired(required = false)
    private RouteTenantKeyConverter tenantKeyConverter;
	@Autowired(required = false)
    private DataSourceConfigLoader dataSourceConfigLoader;

	public DynamicMultiDataSource() {
		this(DataSourceConfig.DEFAULT_GROUP_NAME);
	}

	public DynamicMultiDataSource(String group) {
		this.group = group;
		schemaTenantMode = MybatisConfigs.isSchameSharddingTenant(group);
		mixTenantDataSourceKey = group + "-default_tenant_datasource_key_";
		
		if(schemaTenantMode) {
			missTenantDataSourceKey = MybatisConfigs.getProperty(group, "mybatis.defaultTenantDataSourceKey.onTenantIdMissing", null);
		}
	}
	
	public String getGroup() {
		return group;
	}

	public static Set<String> getAllTenantDataSourceKeys() {
		return Collections.unmodifiableSet(allTenantDataSourceKeys);
	}
	
	public Map<String, DataSource> getTargetDataSources() {
		return Collections.unmodifiableMap(targetDataSources);
	}
	
	public Map<String, DataSourceConfig> getTargetDataSourceConfigs() {
		return targetDataSourceConfigs;
	}

	public void setTargetDataSourceConfigs(Map<String, DataSourceConfig> targetDataSourceConfigs) {
		this.targetDataSourceConfigs = targetDataSourceConfigs;
	}

	public static String dataSourceKeyToTenantId(String dataSourceKey) {
		Collection<DynamicMultiDataSource> dataSources = InstanceFactory.getBeansOfType(DynamicMultiDataSource.class).values();
		for (DynamicMultiDataSource dataSource : dataSources) {
			if(!dataSource.schemaTenantMode)continue;
			Set<String> tenantIds = dataSource.dataSourceKeyToTenantMapping.get(dataSourceKey);
			if(tenantIds == null || tenantIds.isEmpty()) {
				return dataSource.cacheTenantIdToKeyMapping(dataSourceKey,false);
			}else if(tenantIds.size() == 1) {
				return tenantIds.stream().findFirst().orElse(null);
			}
		}
	    return null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		List<DataSourceConfig> dsConfigs = DataSoureConfigHolder.getConfigs(group);
		if(dataSourceConfigLoader != null) {
			dsConfigs.addAll(dataSourceConfigLoader.load(group));
		}
		
		Validate.isTrue(!dsConfigs.isEmpty(), "Not Any DataSource Config Found");

		boolean hasNoTenantDataSource = false;
		for (DataSourceConfig dataSourceConfig : dsConfigs) {	
			registerRealDataSource(dataSourceConfig);
			//
			if(!hasNoTenantDataSource) {
				hasNoTenantDataSource = StringUtils.isBlank(dataSourceConfig.getTenantRouteKey());
			}
		}
		//租户库隔离和字段隔离混合使用时,定时任务会用到
		if(hasNoTenantDataSource 
				&& !tenantDataSourceKeys.isEmpty() 
				&& StringUtils.isNotBlank(MybatisConfigs.getTenantColumnName(group))) {
			tenantDataSourceKeys.add(mixTenantDataSourceKey);
			allTenantDataSourceKeys.add(mixTenantDataSourceKey);
		}
	
		
		if (this.targetDataSources == null || targetDataSources.isEmpty()) {
			throw new IllegalArgumentException("Property 'targetDataSources' is required");
		}
		
		logger.info(">> init multiRouteDataSource[{}] finished ->\n- schemaTenantMode:{}\n -targetDataSources:{}\n -tenantDataSourceKeys:{}\n -slaveNumsMap:{}"
				,group
				,schemaTenantMode
				,targetDataSources.keySet()
				,tenantDataSourceKeys
				,slaveNumsMap);
	}

	private String currentDataSourceKey() {
		DataSourceContextVals context = MybatisRuntimeContext.getDataSourceContextVals();
		boolean useMaster = context.master == null ? true : context.master;
		int index = 0;
		String tenantId = schemaTenantMode ? context.tenantId : null;
		String tenantDataSourceKey = schemaTenantMode ? context.tenantDataSourceKey : null;
		if(schemaTenantMode 
				&& (StringUtils.isBlank(tenantId) || GlobalConstants.VIRTUAL_TENANT_ID.equals(tenantId))
				&& StringUtils.isBlank(tenantDataSourceKey)) {
			if(missTenantDataSourceKey != null) {
				tenantDataSourceKey = missTenantDataSourceKey;
			}else {				
				throw new MendmixBaseException("context:tentantId is missing!!!");
			}
		}
		
		if(tenantDataSourceKey == null && tenantId != null) {
			//租户id -> 数据源定义的租户标识（租户id或租户编码）
			tenantDataSourceKey = tenanIdToTenantDataSourceKey(tenantId);
			//兼容混合模式下字段隔离
			if(tenantDataSourceKey != null && schemaTenantMode && !tenantDataSourceKeys.contains(tenantDataSourceKey)) { 
				tenantDataSourceKey = null;
			}
		}else if(mixTenantDataSourceKey.equals(tenantDataSourceKey)) {
			//匹配默认数据源
			tenantDataSourceKey = null;
		}
		
		if(!useMaster) {
			if(slaveNumsMap.isEmpty()) {
				useMaster = true;
			}else {
				String subGroup = group;
				if(tenantDataSourceKey != null) {
					subGroup = new StringBuilder().append(group).append(GlobalConstants.UNDER_LINE).append(tenantDataSourceKey).toString();
				}
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
		//根据tenantDataSourceKey反向设置当前租户
		if(tenantId == null 
				&& tenantDataSourceKey != null 
				&& dataSourceKeyToTenantMapping.containsKey(tenantDataSourceKey)
				&& dataSourceKeyToTenantMapping.get(tenantDataSourceKey).size() == 1) {
			tenantId = dataSourceKeyToTenantMapping.get(tenantDataSourceKey).stream().findFirst().orElse(null);
		    CurrentRuntimeContext.setTenantId(tenantId);
		}
		//
		String dataSourceKey = DataSourceConfig.buildDataSourceKey(group, tenantDataSourceKey, useMaster, index);
		if(logger.isDebugEnabled() || CurrentRuntimeContext.isDebugMode()) {
			logger.info("<trace_logging> currentDataSourceKey:{},tenantId:{}, useMaster:{}",dataSourceKey,tenantId,useMaster);
		}
		return dataSourceKey;
	}
	
	private String tenanIdToTenantDataSourceKey(String tenantId) {
		if(tenantKeyConverter == null) {
			synchronized (this) {
				if(tenantKeyConverter == null) {
					tenantKeyConverter = InstanceFactory.getInstance(RouteTenantKeyConverter.class);
				}
			}
		}
		//
		if(tenantKeyConverter.caching() && tenantIdToKeyMapping.containsKey(tenantId)) {
			return tenantIdToKeyMapping.get(tenantId);
		}
		String tenantKey = tenantKeyConverter.convert(group, tenantId);
		if(StringUtils.isBlank(tenantKey)) {
			tenantKey = tenantId;
		}
		if(tenantKeyConverter.caching()) {
			if(!StringUtils.equals(tenantKey, tenantId)) {
				logger.info(">> add tenanIdToTenantDataSourceKey mapping:{} = {}",tenantId,tenantKey);
			}
			tenantIdToKeyMapping.put(tenantId, tenantKey);
		}
		//
		addDataSourceKeyToTenantMapping(tenantKey, tenantId);
		return tenantKey;
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
	    	logger.warn("not_match_dataSource for:{},allDataSourceKeys:{}",lookupKey,targetDataSources.keySet());
			throw new MendmixBaseException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
		}
		return dataSource;
	}

	/**
	 * 功能说明：根据DataSource创建bean并注册到容器中
	 * @param config
	 * @throws SQLException 
	 */
	private void registerRealDataSource(DataSourceConfig config) throws SQLException {
		
		config.validate();
		//解密
		if(config.getPassword().startsWith(GlobalConstants.CRYPT_PREFIX)) {
			String password = EnvironmentHelper.decryptConfigValue(config.getPassword());
			config.setPassword(password);
		}
		
		DataSource dataSource = null;
		if (DataSourceType.druid == dataSourceType) {
			dataSource = DruidDataSourceBuilder.builder(config);
		} else if (DataSourceType.hikariCP == dataSourceType) {
			dataSource = HikariCPDataSourceBuilder.builder(config);
		}

		String dsKey = config.dataSourceKey();
		targetDataSourceConfigs.put(dsKey, config);
		targetDataSources.put(dsKey, dataSource);
		logger.info(">>registerRealDataSource \n - dsKey:{}\n - dataSource:{}",dsKey,config.getUrl());
		//保存slave节点数
		if(dsKey.contains(DataSourceConfig.SLAVE_KEY)) {
			String subGroup = StringUtils.splitByWholeSeparator(dsKey, "_slave")[0];
			if(slaveNumsMap.containsKey(subGroup)) {
				slaveNumsMap.put(subGroup, slaveNumsMap.get(subGroup) + 1);
			}else {
				slaveNumsMap.put(subGroup, 1);
			}
		}
		//租户独立数据源
		if(config.getTenantRouteKey() != null) {
			tenantDataSourceKeys.add(config.getTenantRouteKey());
			allTenantDataSourceKeys.add(config.getTenantRouteKey());
			//
			if(!config.getScopeTenantIds().isEmpty()) {
				for (String tenantId : config.getScopeTenantIds()) {						
					tenantIdToKeyMapping.put(tenantId, config.getTenantRouteKey());
					addDataSourceKeyToTenantMapping(config.getTenantRouteKey(), tenantId);
				}
			}else {
				cacheTenantIdToKeyMapping(config.getTenantRouteKey(), false);
			}
		}
		//发布事件
		InstanceFactory.getContext().publishEvent(new CommonApplicationEvent(SpringEventType.tenantDataSourceChanged, allTenantDataSourceKeys));
	}
	 
	private String cacheTenantIdToKeyMapping(String tenantRouteKey,boolean handleError) {
		String tenantId = null;
		synchronized (tenantIdToKeyMapping) {
			if(tenantIdToKeyMapping.values().contains(tenantRouteKey)) {
				return tenantIdToKeyMapping.entrySet().stream().filter(o -> StringUtils.equals(tenantRouteKey, o.getValue())).findFirst().get().getKey();
			}
			if(StringUtils.isNumeric(tenantRouteKey)) {	
				tenantId = tenantRouteKey;
			}else {
				//TODO 
			}
			if(tenantId != null) {
				tenantIdToKeyMapping.put(tenantId, tenantRouteKey);
				addDataSourceKeyToTenantMapping(tenantRouteKey, tenantId);
			}
		}
		return tenantId;
	}

	@Override
	public void destroy() throws Exception {
		Collection<DataSource> dataSources = targetDataSources.values();
		for (DataSource dataSource : dataSources) {
			try {
				((DruidDataSource)dataSource).close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void updateDataSource(DataSourceConfig dataSourceConfig) {
		String dataSourceKey = dataSourceConfig.dataSourceKey();
		if(targetDataSources.containsKey(dataSourceKey)) {
			//仅更新租户和数据源关系
			if(StringUtils.isBlank(dataSourceConfig.getTenantRouteKey())) {
				return;
			}
			if(!dataSourceConfig.getScopeTenantIds().isEmpty()) {
				for (String tenantId : dataSourceConfig.getScopeTenantIds()) {						
					tenantIdToKeyMapping.put(tenantId, dataSourceConfig.getTenantRouteKey());
				}
			}
		}else {
			try {				
				registerRealDataSource(dataSourceConfig);
				logger.info(">>完成动态注册数据源:{}",dataSourceConfig);
			} catch (Exception e) {
				logger.error(">>动态新增数据源错误>>\n"+dataSourceConfig.toString(),e);
			}
		}
	}

	private void addDataSourceKeyToTenantMapping(String dsKey,String tenantId) {
		Set<String> list = dataSourceKeyToTenantMapping.get(dsKey);
		if(list == null) {
			list = new HashSet<>();
			dataSourceKeyToTenantMapping.put(dsKey, list);
		}
		if(!list.contains(tenantId)) {
			list.add(tenantId);
		}
	}

} 
