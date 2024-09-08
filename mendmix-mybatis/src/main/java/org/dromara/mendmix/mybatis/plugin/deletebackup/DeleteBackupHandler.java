package org.dromara.mendmix.mybatis.plugin.deletebackup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.persistence.Table;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import org.dromara.mendmix.common.util.CachingFieldUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.core.BaseEntity;
import org.dromara.mendmix.mybatis.core.BaseMapper;
import org.dromara.mendmix.mybatis.crud.CrudMethods;
import org.dromara.mendmix.mybatis.kit.MapperBeanHolder;
import org.dromara.mendmix.mybatis.kit.MybatisMapperParser;
import org.dromara.mendmix.mybatis.kit.MybatisSqlRewriteUtils;
import org.dromara.mendmix.mybatis.metadata.ColumnMetadata;
import org.dromara.mendmix.mybatis.metadata.EntityMetadata;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.dromara.mendmix.mybatis.metadata.MetadataHelper;
import org.dromara.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.OnceInterceptorStrategy;
import org.dromara.mendmix.mybatis.plugin.PluginInterceptorHandler;
import org.dromara.mendmix.mybatis.plugin.deletebackup.annotation.DeleteBackup;
import org.dromara.mendmix.spring.InstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年7月8日
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class DeleteBackupHandler implements PluginInterceptorHandler {


	private static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");
	private static final String CTX_DELETED_ENTITIES = "__ctx_deleted_entities_";

	private Map<String, String> backupTableNameMapping = new HashMap<>();
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	
	private boolean asyncMode;
	private LinkedBlockingQueue<UpdateTask> asyncTaskQueue;
	private  ScheduledExecutorService executor;
	private List<UpdateTask> asyncTaskTransferList;
	
	@Override
	public void start(MendmixMybatisInterceptor context) {
		dataSource = context.getDataSource();
		asyncMode = ResourceUtils.getBoolean("mendmix-cloud.mybatis.deletebackup.asyncMode",true);
		if(asyncMode) {
			asyncTaskQueue = new LinkedBlockingQueue<>(5000);
			asyncTaskTransferList  = new ArrayList<>(20);
			executor = Executors.newScheduledThreadPool(1,new StandardThreadFactory("asyncDeleteBackupScheduler"));
			executor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					if(asyncTaskQueue.isEmpty())return;
					asyncTaskTransferList.clear();
					asyncTaskQueue.drainTo(asyncTaskTransferList, 20);
					if(!asyncTaskTransferList.isEmpty()) {
						String entityName = asyncTaskTransferList.get(0).getClass().getSimpleName();
						logger.info(">>asyncDeleteBackup BEGIN  -> entityName:{},size:{}",entityName,asyncTaskTransferList.size());
					}
				    long currentTime = System.currentTimeMillis();
					for (UpdateTask task : asyncTaskTransferList) {
						//避免事务问题，延迟处理
						if(currentTime - task.operatorTime.getTime() < 5000) {
							asyncTaskQueue.add(task);
						}else {	
							updateOperatorInfo(task.list, task.operatorBy, task.operatorTime);
						}
					}
				}
			}, 10000, 5000, TimeUnit.MILLISECONDS);
		}
		
		if(InstanceFactory.getBeanCountOfType(JdbcTemplate.class) == 1) {
			jdbcTemplate = InstanceFactory.getInstance(JdbcTemplate.class);
		}else {			
			jdbcTemplate = InstanceFactory.getInstance(context.getGroupName() + "JdbcTemplate");
		}
		List<MapperMetadata> list = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		String backupTable;
		for (MapperMetadata entityInfo : list) {
			if(entityInfo.getEntityClass().isAnnotationPresent(DeleteBackup.class)) {
				backupTable = entityInfo.getEntityClass().getAnnotation(DeleteBackup.class).backupTo();
			    if(StringUtils.isBlank(backupTable)) {
			    	backupTable = entityInfo.getTableName() + "_deleted_his";
			    }
			    backupTableNameMapping.put(entityInfo.getTableName(), backupTable);
			}
		}
	}

	@Override
	public void close() {}

	@Override
	public Object onInterceptor(OnceContextVal invocationVal) throws Throwable {
		MappedStatement mt = invocationVal.getMappedStatement();
		if(!SqlCommandType.DELETE.equals(mt.getSqlCommandType()))return null;
		MapperMetadata entityInfo = invocationVal.getEntityInfo();
		if(entityInfo == null || !backupTableNameMapping.containsKey(entityInfo.getTableName()))return null;
		//更新的对象ids
		List<Object> ids;
		final String mtId = invocationVal.getMappedStatement().getId();
		if(mtId.endsWith(CrudMethods.deleteByPrimaryKey.name())){
			ids = Arrays.asList(invocationVal.getParameter());
		}else {
			ids = MybatisSqlRewriteUtils.dynaQueryConditionToIdList(dataSource,invocationVal);
		}
		if(CurrentRuntimeContext.isDebugMode()) {
			logger.info("<debug_trace_logging> deleteBackupIdList ids:{}",ids);
		}
		if(!ids.isEmpty()) {
			BaseMapper baseMapper = MapperBeanHolder.getMappeBean(invocationVal.getMapperNameSpace());
			List<? extends BaseEntity> list;
			list = OnceInterceptorStrategy.apply().ignoreRwRoute().ignoreTenant().ignoreSoftDeleteConditon().ignoreDataPermission().exec(() -> {
				if(ids.size() == 1) {
					BaseEntity entity = baseMapper.selectByPrimaryKey(ids.get(0).toString());
					if(entity != null)return Arrays.asList(entity);
				}else {
					return baseMapper.selectByPrimaryKeys(ids);
				}
				return null;
			});
			//放入上下文
			if(list != null && !list.isEmpty()) {
				ThreadLocalContext.set(CTX_DELETED_ENTITIES, list);
				logger.debug(">>method[{}] 生成预删除记录:{}",mt.getId(),list.size());
			}
		}
		return null;
	}

	
	@Override
	public void onFinished(OnceContextVal invocationVal, Object result) {
		if(!SqlCommandType.DELETE.equals(invocationVal.getMappedStatement().getSqlCommandType()))return;
		List<? extends BaseEntity> list = ThreadLocalContext.get(CTX_DELETED_ENTITIES);
		Map<String, String> oldRewriteTableNameRules = null;
		try {
			if(list == null || list.isEmpty())return;
			//更新时间
			setUpdatedFieldValues(list);
			//表名重写规则
			oldRewriteTableNameRules = MybatisRuntimeContext.getRewriteTableNameRules();
			BaseMapper baseMapper = MapperBeanHolder.getMappeBean(invocationVal.getMapperNameSpace());
			//设置重写规则
			MybatisRuntimeContext.setRewriteTableNameRules(backupTableNameMapping);
			OnceInterceptorStrategy.apply().ignoreTableSharding().ignoreLoggingDataChange().exec(()-> {
				if(list.size() == 1) {
					baseMapper.insert(list.get(0));
				}else {
					baseMapper.insertList(list);
				}
				logger.debug(">>method[{}] 插入硬删除备份记录:{}",invocationVal.getMappedStatement().getId(),list.size());
				return null;
			});
			//操作人相关 
			String operatorBy = CurrentRuntimeContext.getCurrentUserId();
			Date operatorTime = new Date();
			if(asyncMode) {
				UpdateTask task = new UpdateTask(list, operatorBy, operatorTime);
				try {					
					asyncTaskQueue.add(task);
				} catch (Exception e) {
					updateOperatorInfo(list, operatorBy, operatorTime);
				}
			}else {
				updateOperatorInfo(list, operatorBy, operatorTime);
			}
		}catch (Exception e) {
			logger.error("insertBackupData_ERROR",e);
		} finally {
			ThreadLocalContext.remove(CTX_DELETED_ENTITIES);
			MybatisRuntimeContext.setRewriteTableNameRules(oldRewriteTableNameRules);
		}
		
	}
	
	@Override
	public boolean compatibleSqlRewrite() {
		return true;
	}

	private void setUpdatedFieldValues(List<? extends BaseEntity> list) {
		EntityMetadata entityMapper = MetadataHelper.getEntityMapper(list.get(0).getClass());
		if(entityMapper != null) {
			String updatedAtField = null;
			String updatedByField = null;
			for (ColumnMetadata column : entityMapper.getColumnsMapper()) {
				if(column.isUpdatedAtField()) {
					updatedAtField = column.getProperty();
				}
				if(column.isUpdatedByField()) {
					updatedByField = column.getProperty();
				}
			}
			for (BaseEntity entity : list) {
				if(updatedAtField != null) {
					CachingFieldUtils.writeField(entity, updatedAtField, new Date());
				}
				if(updatedByField != null) {
					CachingFieldUtils.writeField(entity, updatedByField, MybatisRuntimeContext.getCurrentUserId());
				}
			}
		}
	}

	@Override
	public int interceptorOrder() {
		return 9;
	}
	

	private void updateOperatorInfo(List<? extends BaseEntity> list,String operatorBy,Date operatorTime) {
		Class<? extends BaseEntity> entityClass = list.get(0).getClass();
		DeleteBackup annotation = entityClass.getAnnotation(DeleteBackup.class);
		boolean updateBy = operatorBy != null && StringUtils.isNotBlank(annotation.updateByColumn());
		boolean updateTime = StringUtils.isNotBlank(annotation.updateTimeColumn());
		if(!updateBy && !updateTime) {
			return;
		}
		Table tableAnnotation = entityClass.getAnnotation(Table.class);
		String tableName = backupTableNameMapping.get(tableAnnotation.name());
		StringBuilder sb = new StringBuilder();
		List<Object> params = new ArrayList<>(0);
		sb.append("UPDATE ").append(tableName).append(" SET ");
		if(updateBy) {
			sb.append(annotation.updateByColumn()).append(" = ?");
			params.add(operatorBy);
		}
		if(updateTime) {
			if(updateBy)sb.append(", ");
			sb.append(annotation.updateTimeColumn()).append(" = ?");
			params.add(operatorTime);
		}
		sb.append(" WHERE id");
		if(list.size() == 1) {
			sb.append(" = ?");
			params.add(list.get(0).getId());
		}else {
			sb.append(" IN(");
			for (BaseEntity entity : list) {
				sb.append("?,");
				params.add(entity.getId());
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")");
		}
		jdbcTemplate.update(sb.toString(), params.toArray());
	}

	private class UpdateTask{
		String operatorBy;
		Date operatorTime;
		List<? extends BaseEntity> list;
		public UpdateTask(List<? extends BaseEntity> list,String operatorBy, Date operatorTime) {
			super();
			this.operatorBy = operatorBy;
			this.operatorTime = operatorTime;
			this.list = list;
		}
	}
}
