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
package org.dromara.mendmix.mybatis.plugin.datalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import org.dromara.mendmix.common.constants.DataChangeType;
import org.dromara.mendmix.common.model.ModifiedObject;
import org.dromara.mendmix.common.util.ExceptionFormatUtils;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.core.BaseEntity;
import org.dromara.mendmix.mybatis.core.BaseMapper;
import org.dromara.mendmix.mybatis.crud.CrudMethods;
import org.dromara.mendmix.mybatis.kit.MapperBeanHolder;
import org.dromara.mendmix.mybatis.kit.MybatisMapperParser;
import org.dromara.mendmix.mybatis.kit.MybatisSqlRewriteUtils;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.dromara.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.OnceInterceptorStrategy;
import org.dromara.mendmix.mybatis.plugin.PluginInterceptorHandler;
import org.dromara.mendmix.mybatis.plugin.datalog.annotation.DataChangeLogeable;
import org.dromara.mendmix.spring.InstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据变更日志mybatis层拦截处理器
 * <br>
 * Class Name   : DataChangeLogHandler
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年10月26日
 */
public class DataChangeLogHandler implements PluginInterceptorHandler {

	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");

	private static final String CTX_BEFORE_CHANGE_ENTITIES = "_ctx_before_change_entities";

	private DataSource dataSource;
	
	private static PriorityBlockingQueue<ModifiedObject> taskQueue;
	private static ScheduledExecutorService syncEventHandler;
	
	private DataChangeLogPublisher dataChangeLogPublisher;
	
	private int delayMillis = ResourceUtils.getInt("mendmix-cloud.mybatis.changelog.delayMillis", 5000);
	private boolean enabled = false;
	
	private DataChangeLogPublisher getDataChangeLogPublisher() {
		if(dataChangeLogPublisher != null)return dataChangeLogPublisher;
		synchronized (DataChangeLogHandler.class) {
			if(dataChangeLogPublisher != null)return dataChangeLogPublisher;
			dataChangeLogPublisher = InstanceFactory.getInstance(DataChangeLogPublisher.class);
			if(dataChangeLogPublisher == null) {
				dataChangeLogPublisher = new DataChangeLogPublisher() {
					@Override
					public boolean publish(DataChangeItem item) {
						logger.warn("DataChangeLogPublisher NOT FOUND,Ignore dataChangeLog:\n - {}",JsonUtils.toJson(item));
						return true;
					}
				};
			}
		}
		return dataChangeLogPublisher;
	}



	@Override
	public void start(MendmixMybatisInterceptor context) {
		dataSource = context.getDataSource();
		List<MapperMetadata> list = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		enabled = list.stream().anyMatch(o -> o.getEntityClass().isAnnotationPresent(DataChangeLogeable.class));
		if(!enabled) {
			return;
		}
		//
		if(taskQueue == null) {
			taskQueue = new PriorityBlockingQueue<>(5000);
			syncEventHandler = Executors.newScheduledThreadPool(1,new StandardThreadFactory("dataChangeLogEventHandler"));
			syncEventHandler.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					if(taskQueue.isEmpty())return;
					ModifiedObject object = taskQueue.poll();
			        if(object == null)return;
				    //避免事务未提交，延迟处理
					if(System.currentTimeMillis() - object.getLastModifiedTime() < delayMillis) {
						taskQueue.add(object);
					}else {
						handleDataChange(object.getObject());
					}
				}
			}, 1000, 100, TimeUnit.MILLISECONDS);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object onInterceptor(OnceContextVal invocation) throws Throwable {
		if(!enabled || MybatisRuntimeContext.isIgnoreLoggingDataChange())return null;
		MappedStatement mt = invocation.getMappedStatement();
		final SqlCommandType sqlType = mt.getSqlCommandType();
		if(SqlCommandType.SELECT.equals(sqlType))return null;
		try {
			if(!invocation.getEntityInfo().getEntityClass().isAnnotationPresent(DataChangeLogeable.class)) {
				return null;
			}
			final Object parameter = invocation.getParameter();
			List<Object> ids = null;
			if(mt.getId().endsWith(CrudMethods.deleteByPrimaryKey.name())) {
				ids = Arrays.asList(parameter);
			}else if(mt.getId().endsWith(CrudMethods.deleteByPrimaryKeys.name()) 
					|| mt.getId().endsWith(CrudMethods.batchLogicDelete.name())
					|| mt.getId().endsWith(CrudMethods.batchUpdateByPrimaryKeys.name())) {
				ids = (List<Object>) parameter;
			}else if(mt.getId().endsWith(CrudMethods.updateByPrimaryKey.name()) 
					|| mt.getId().endsWith(CrudMethods.updateByPrimaryKeySelective.name())
					|| mt.getId().endsWith(CrudMethods.updateByPrimaryKeyWithVersion.name())) {
				ids = Arrays.asList(((BaseEntity)parameter).getId());
			}else if(mt.getId().endsWith(CrudMethods.updateListByPrimaryKeys.name()) 
					|| mt.getId().endsWith(CrudMethods.updateListByPrimaryKeysSelective.name())) {
				List<? extends BaseEntity> entities = (List<? extends BaseEntity>)((Map<String, Object>)parameter).get("arg0");
				ids = new ArrayList<>(entities.size());
				for (BaseEntity entity : entities) {
					ids.add(entity.getId());
				}
			}else if(!SqlCommandType.INSERT.equals(sqlType)){
				if(!invocation.getEntityInfo().getEntityClass().isAnnotationPresent(DataChangeLogeable.class)) {
					return null;
				}
				ids = MybatisSqlRewriteUtils.dynaQueryConditionToIdList(dataSource,invocation);
			}
			//
			List<? extends BaseEntity> beforeEntities = null;
			if(mt.getId().endsWith(CrudMethods.insert.name()) 
					|| mt.getId().endsWith(CrudMethods.insertSelective.name())) {
				beforeEntities = Arrays.asList((BaseEntity)parameter);
			}else if(mt.getId().endsWith(CrudMethods.insertList.name())) {
				beforeEntities = (List<? extends BaseEntity>)((Map<String, Object>)parameter).get("arg0");
			}else if(ids != null && !ids.isEmpty() && !SqlCommandType.DELETE.equals(sqlType)) {
				BaseMapper baseMapper = MapperBeanHolder.getMappeBean(invocation.getMapperNameSpace());
				List<Object> _ids = ids;
				beforeEntities = OnceInterceptorStrategy.apply() //
						 .ignoreRwRoute()//
						 .ignoreTenant()//
						 .ignoreSoftDeleteConditon()//
						 .ignoreDataPermission()//
						 .exec(() -> baseMapper.selectByPrimaryKeys(_ids));
			}
			
			if(beforeEntities != null || ids != null) {
				String mapperName = invocation.getMapperNameSpace();
				DataSnapshot snapshot = new DataSnapshot(mt.getId(),mapperName,ids,beforeEntities);
				ThreadLocalContext.set(CTX_BEFORE_CHANGE_ENTITIES, snapshot);
				if(logger.isDebugEnabled())logger.debug(">>生成变更前快照信息:{}",snapshot);
			}
		} catch (Exception e) {
			logger.error(">>生成变更前快照错误<build_before_snapshot_error> -> mapper:{},error:{}",mt.getId(),ExceptionFormatUtils.buildExceptionMessages(e));
		}
		return null;
	}

	@Override
	public void onFinished(OnceContextVal invocation, Object result) {
		if(!enabled || MybatisRuntimeContext.isIgnoreLoggingDataChange())return;
		final MappedStatement mt = invocation.getMappedStatement();
		if(SqlCommandType.SELECT.equals(mt.getSqlCommandType()))return;
		if(!ThreadLocalContext.exists(CTX_BEFORE_CHANGE_ENTITIES)) {
			return;
		}
		DataSnapshot snapshot = ThreadLocalContext.getAndRemove(CTX_BEFORE_CHANGE_ENTITIES);
		if(!mt.getId().equals(snapshot.sourceMethod))return;
		taskQueue.add(new ModifiedObject(snapshot));
		//放入redis??
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void handleDataChange(DataSnapshot snapshot) {
		MapperMetadata entityInfo = MybatisMapperParser.getMapperMetadata(snapshot.mapperName);
		//
		int changeItemNums = snapshot.ids == null ? snapshot.entities.size() : snapshot.ids.size();
		List<DataChangeItem> changeItems = new ArrayList<>(changeItemNums);
		DataChangeItem changeItem;
		Serializable id = null;
		BaseEntity oldValue = null;
		String pubEntityName = getEntityName(entityInfo.getEntityClass());
		for (int i = 0; i < changeItemNums; i++) {
			id = snapshot.ids == null ? null : (Serializable)snapshot.ids.get(i);
			oldValue = snapshot.entities == null ? null : snapshot.entities.get(i);
            if(id == null && oldValue != null) {
				id = oldValue.getId();
			}
			//
			changeItem = new DataChangeItem(pubEntityName, id.toString(), snapshot.changeType, oldValue);
			if(snapshot.changeType == DataChangeType.update) {
				BaseMapper baseMapper = MapperBeanHolder.getMappeBean(snapshot.mapperName);
				Serializable _id = id;
				//TODO 分表上下文？？
				BaseEntity newValue = OnceInterceptorStrategy.apply() //
						 .ignoreRwRoute()//
						 .ignoreTenant()//
						 .ignoreSoftDeleteConditon()//
						 .ignoreDataPermission()//
						 .exec(() -> baseMapper.selectByPrimaryKey(_id));
				if(newValue == null) {
					continue;
				}
				changeItem.updateNewValue(newValue);
			} 
			changeItems.add(changeItem);
		}
		//
		for (DataChangeItem item : changeItems) {
			if(DataChangeType.update == item.getChangeType() 
					&& (item.getChangeFields() == null || item.getChangeFields().isEmpty())) {
				continue;
			}
			getDataChangeLogPublisher().publish(item);
			if(logger.isDebugEnabled()) {
				logger.debug(">>完成发布变更事件 ->changeType:{},id:{}",item.getChangeType(),item.getId());
			}
		}
	}
	
	private String getEntityName(Class<?> entityClass) {
		String entityName;
		final DataChangeLogeable annotation = entityClass.getAnnotation(DataChangeLogeable.class);
		if(annotation == null)return null;
		String[] aliasName = annotation.entityAliasName();
		if(aliasName.length == 1) {
			entityName = aliasName[0];
		}else {
			entityName = entityClass.getSimpleName();
		}
		return entityName;
	}

	@Override
	public int interceptorOrder() {
		return 9;
	}
	
	@Override
	public boolean compatibleSqlRewrite() {
		return true;
	}

	@Override
	public void close() {
		if(syncEventHandler != null)syncEventHandler.shutdown();
	}

}
