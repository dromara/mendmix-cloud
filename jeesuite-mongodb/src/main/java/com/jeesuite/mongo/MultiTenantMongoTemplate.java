package com.jeesuite.mongo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.DbCallback;
import org.springframework.data.mongodb.core.DocumentCallbackHandler;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ScriptOperations;
import org.springframework.data.mongodb.core.SessionScoped;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexOperationsProvider;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapreduce.GroupBy;
import org.springframework.data.mongodb.core.mapreduce.GroupByResults;
import org.springframework.data.mongodb.core.mapreduce.MapReduceOptions;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.data.util.CloseableIterator;

import com.jeesuite.common.JeesuiteBaseException;
import com.mongodb.ClientSessionOptions;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * 多租户MongoTemplate
 * <br>
 * Class Name   : MultiTenantMongoTemplate
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月29日
 */
public class MultiTenantMongoTemplate implements MongoOperations, ApplicationContextAware, IndexOperationsProvider{

	private static Logger logger = LoggerFactory.getLogger("com.zyframework.core.mongodb");
	
	private static Map<String, MongoTemplate> realTenantTemplateMappings = new HashMap<>();
	private static Map<String, String> classNameRCollectionNameMappings = new HashMap<>();
	
	public MultiTenantMongoTemplate() {}

	private static String getClassCollectionName(Class<?> clazz){
    	
    	if(classNameRCollectionNameMappings.containsKey(clazz.getName())){
    		return classNameRCollectionNameMappings.get(clazz.getName());
    	}
    	String collection;
    	if(clazz.isAnnotationPresent(org.springframework.data.mongodb.core.mapping.Document.class)){
    		collection = clazz.getAnnotation(org.springframework.data.mongodb.core.mapping.Document.class).collection();
    	}else{
    		throw new RuntimeException("实体["+clazz.getCanonicalName()+"]未找到@Document注解");
    	}
    	classNameRCollectionNameMappings.put(clazz.getName(), collection);
    	return collection;
    }
	
	
	private void publishRefreshCacheEvent(String collectionName,Object arg){
		String id = null;
		if(arg != null){
			if(arg instanceof BaseObject){
				id = ((BaseObject)arg).getId();
			}else if(arg instanceof Query){
				id = ((Query)arg).getQueryObject().getString(CommonFields.ID_FIELD_NAME);
			}else{
				id = arg.toString();
			}
		}
	}

	public static void addMongoDbFactory(String tenantId,MongoDbFactory mongoDbFactory) {
		MongoConverter converter = getDefaultMongoConverter(mongoDbFactory);
		MongoTemplate template = new MongoTemplate(mongoDbFactory,converter);
		realTenantTemplateMappings.put(tenantId, template);
	}
	
	private MongoTemplate getRealMongoTemplate(String tenantId){
		if(!realTenantTemplateMappings.containsKey(tenantId)){
			throw new JeesuiteBaseException("无租户["+tenantId+"]数据源");
		}
		return realTenantTemplateMappings.get(tenantId);
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		realTenantTemplateMappings.values().forEach(template -> {
			template.setApplicationContext(applicationContext);
		});
	}

	@Override
	public <T> ExecutableFind<T> query(Class<T> domainType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).query(domainType);
	}

	@Override
	public <T> ExecutableInsert<T> insert(Class<T> domainType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).insert(domainType);
	}

	@Override
	public <T> ExecutableUpdate<T> update(Class<T> domainType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).update(domainType);
	}


	@Override
	public <T> ExecutableRemove<T> remove(Class<T> domainType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).remove(domainType);
	}

	@Override
	public <T> ExecutableAggregation<T> aggregateAndReturn(Class<T> domainType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).aggregateAndReturn(domainType);
	}

	@Override
	public <T> MapReduceWithMapFunction<T> mapReduce(Class<T> domainType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).mapReduce(domainType);
	}

	@Override
	public String getCollectionName(Class<?> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).getCollectionName(entityClass);
	}

	@Override
	public Document executeCommand(String jsonCommand) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).executeCommand(jsonCommand);
	}

	@Override
	public Document executeCommand(Document command) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).executeCommand(command);
	}

	@Override
	public Document executeCommand(Document command, ReadPreference readPreference) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).executeCommand(command, readPreference);
	}

	@Override
	public void executeQuery(Query query, String collectionName, DocumentCallbackHandler dch) {
		String tenantId = TenantHolder.getTenantId();
		getRealMongoTemplate(tenantId).executeQuery(query, collectionName, dch);
	}

	@Override
	public <T> T execute(DbCallback<T> action) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).execute(action);
	}

	@Override
	public <T> T execute(Class<?> entityClass, CollectionCallback<T> action) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).execute(entityClass, action);
	}

	@Override
	public <T> T execute(String collectionName, CollectionCallback<T> action) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).execute(collectionName, action);
	}


	@Override
	public SessionScoped withSession(ClientSessionOptions sessionOptions) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).withSession(sessionOptions);
	}

	@Override
	public <T> CloseableIterator<T> stream(Query query, Class<T> entityType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).stream(query, entityType);
	}

	@Override
	public <T> CloseableIterator<T> stream(Query query, Class<T> entityType, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).stream(query, entityType);
	}


	@Override
	public Set<String> getCollectionNames() {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).getCollectionNames();
	}


	@Override
	public <T> boolean collectionExists(Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).collectionExists(entityClass);
	}


	@Override
	public boolean collectionExists(String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).collectionExists(collectionName);
	}

	@Override
	public <T> void dropCollection(Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		getRealMongoTemplate(tenantId).dropCollection(entityClass);
	}

	@Override
	public void dropCollection(String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		getRealMongoTemplate(tenantId).dropCollection(collectionName);
	}

	@Override
	public IndexOperations indexOps(String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).indexOps(collectionName);
	}

	@Override
	public IndexOperations indexOps(Class<?> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).indexOps(entityClass);
	}

	@Override
	public ScriptOperations scriptOps() {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).scriptOps();
	}

	@Override
	public BulkOperations bulkOps(BulkMode mode, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).bulkOps(mode, collectionName);
	}

	@Override
	public BulkOperations bulkOps(BulkMode mode, Class<?> entityType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).bulkOps(mode, entityType);
	}

	@Override
	public BulkOperations bulkOps(BulkMode mode, Class<?> entityType, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).bulkOps(mode, collectionName);
	}

	@Override
	public <T> List<T> findAll(Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).findAll(entityClass);
	}

	@Override
	public <T> List<T> findAll(Class<T> entityClass, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).findAll(entityClass, collectionName);
	}

	@Override
	public <T> GroupByResults<T> group(String inputCollectionName, GroupBy groupBy, Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).group(inputCollectionName, groupBy, entityClass);
	}

	@Override
	public <T> GroupByResults<T> group(Criteria criteria, String inputCollectionName, GroupBy groupBy,
			Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).group(criteria, inputCollectionName, groupBy, entityClass);
	}


	@Override
	public <O> AggregationResults<O> aggregate(TypedAggregation<?> aggregation, String collectionName,
			Class<O> outputType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).aggregate(aggregation, outputType);
	}


	@Override
	public <O> AggregationResults<O> aggregate(TypedAggregation<?> aggregation, Class<O> outputType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).aggregate(aggregation, outputType);
	}

	@Override
	public <O> AggregationResults<O> aggregate(Aggregation aggregation, Class<?> inputType, Class<O> outputType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).aggregate(aggregation, inputType, outputType);
	}

	@Override
	public <O> AggregationResults<O> aggregate(Aggregation aggregation, String collectionName, Class<O> outputType) {
		String tenantId = TenantHolder.getTenantId();
		AggregationResults<O> aggregate = getRealMongoTemplate(tenantId).aggregate(aggregation, collectionName, outputType);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[aggregate] tenantId:{},collectionName:{},aggregation:{} -> result:{}",tenantId,collectionName,aggregation,aggregate.getMappedResults().size());
		}
		return aggregate;
	}

	@Override
	public <O> CloseableIterator<O> aggregateStream(TypedAggregation<?> aggregation, String collectionName,
			Class<O> outputType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).aggregateStream(aggregation, collectionName, outputType);
	}

	@Override
	public <O> CloseableIterator<O> aggregateStream(TypedAggregation<?> aggregation, Class<O> outputType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).aggregateStream(aggregation, outputType);
	}

	@Override
	public <O> CloseableIterator<O> aggregateStream(Aggregation aggregation, Class<?> inputType, Class<O> outputType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).aggregateStream(aggregation, inputType, outputType);
	}

	@Override
	public <O> CloseableIterator<O> aggregateStream(Aggregation aggregation, String collectionName,
			Class<O> outputType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).aggregateStream(aggregation, collectionName, outputType);
	}

	@Override
	public <T> MapReduceResults<T> mapReduce(String inputCollectionName, String mapFunction, String reduceFunction,
			Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).mapReduce(inputCollectionName, mapFunction, reduceFunction, entityClass);
	}

	@Override
	public <T> MapReduceResults<T> mapReduce(String inputCollectionName, String mapFunction, String reduceFunction,
			MapReduceOptions mapReduceOptions, Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).mapReduce(inputCollectionName, mapFunction, reduceFunction, mapReduceOptions, entityClass);
	}

	@Override
	public <T> MapReduceResults<T> mapReduce(Query query, String inputCollectionName, String mapFunction,
			String reduceFunction, Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).mapReduce(inputCollectionName, mapFunction, reduceFunction, entityClass);
	}


	@Override
	public <T> MapReduceResults<T> mapReduce(Query query, String inputCollectionName, String mapFunction,
			String reduceFunction, MapReduceOptions mapReduceOptions, Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).mapReduce(query, inputCollectionName, mapFunction, reduceFunction, mapReduceOptions, entityClass);
	}


	@Override
	public <T> GeoResults<T> geoNear(NearQuery near, Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).geoNear(near, entityClass);
	}

	@Override
	public <T> GeoResults<T> geoNear(NearQuery near, Class<T> entityClass, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).geoNear(near, entityClass, collectionName);
	}


	@Override
	public <T> T findOne(Query query, Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		T entity = getRealMongoTemplate(tenantId).findOne(query, entityClass);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[findOne] tenantId:{},collectionName:{},query:{} -> result:{}",tenantId,getClassCollectionName(entityClass),query,entity != null);
		}
		return entity;
	}


	@Override
	public <T> T findOne(Query query, Class<T> entityClass, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		T entity = getRealMongoTemplate(tenantId).findOne(query, entityClass, collectionName);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[findOne] tenantId:{},collectionName:{},query:{} -> result:{}",tenantId,collectionName,query,entity != null);
		}
		return entity;
	}


	@Override
	public boolean exists(Query query, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).exists(query, collectionName);
	}

	@Override
	public boolean exists(Query query, Class<?> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).exists(query, entityClass);
	}

	@Override
	public boolean exists(Query query, Class<?> entityClass, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).exists(query, entityClass, collectionName);
	}


	@Override
	public <T> List<T> find(Query query, Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		List<T> entities = getRealMongoTemplate(tenantId).find(query, entityClass);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[find] tenantId:{},collectionName:{},query:{} -> result:{}",tenantId,getClassCollectionName(entityClass),query,entities.size());
		}
		return entities;
	}

	@Override
	public <T> List<T> find(Query query, Class<T> entityClass, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		List<T> entities = getRealMongoTemplate(tenantId).find(query, entityClass, collectionName);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[find] tenantId:{},collectionName:{},query:{} -> result:{}",tenantId,collectionName,query,entities.size());
		}
		return entities;
	}

	@Override
	public <T> T findById(Object id, Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		T entity = getRealMongoTemplate(tenantId).findById(id, entityClass);
		return entity;
	}


	@Override
	public <T> T findById(Object id, Class<T> entityClass, String collectionName) {
		//避免出现class转换异常，自定义entityClass的不自动缓存
		String tenantId = TenantHolder.getTenantId();
		T entity = getRealMongoTemplate(tenantId).findById(id, entityClass, collectionName);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[findById] tenantId:{},collectionName:{},id:{} -> result:{}",tenantId,collectionName,id,entity != null);
		}
		return entity;
	}

	@Override
	public <T> List<T> findDistinct(Query query, String field, Class<?> entityClass, Class<T> resultClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).findDistinct(query, field, entityClass, resultClass);
	}

	@Override
	public <T> List<T> findDistinct(Query query, String field, String collectionName, Class<?> entityClass,
			Class<T> resultClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).findDistinct(query, field, collectionName, entityClass, resultClass);
	}


	@Override
	public <S, T> T findAndReplace(Query query, S replacement, FindAndReplaceOptions options, Class<S> entityType,
			String collectionName, Class<T> resultType) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).findAndReplace(query, replacement, options, entityType, collectionName, resultType);
	}

	@Override
	public <T> T findAndRemove(Query query, Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).findAndRemove(query, entityClass);
	}

	@Override
	public <T> T findAndRemove(Query query, Class<T> entityClass, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).findAndRemove(query, entityClass, collectionName);
	}

	@Override
	public long count(Query query, Class<?> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		long count = getRealMongoTemplate(tenantId).count(query, entityClass);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[count] tenantId:{},collectionName:{},query:{} -> result:{}",tenantId,getClassCollectionName(entityClass),query,count);
		}
		return count;
	}

	@Override
	public long count(Query query, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		long count = getRealMongoTemplate(tenantId).count(query, collectionName);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[count] tenantId:{},collectionName:{},query:{} -> result:{}",tenantId,collectionName,query,count);
		}
		return count;
	}

	@Override
	public long count(Query query, Class<?> entityClass, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		long count = getRealMongoTemplate(tenantId).count(query, entityClass);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[count] tenantId:{},collectionName:{},query:{} -> result:{}",tenantId,collectionName,query,count);
		}
		return count;
	}

	@Override
	public <T> T insert(T objectToSave) {
		String tenantId = TenantHolder.getTenantId();
		String collectionName = getClassCollectionName(objectToSave.getClass());
		T entity = getRealMongoTemplate(tenantId).insert(objectToSave);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[insert] tenantId:{},collectionName:{}",tenantId,collectionName);
		}
		publishRefreshCacheEvent(collectionName, null);
		return entity;
	}

	@Override
	public <T> T insert(T objectToSave, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		T entity = getRealMongoTemplate(tenantId).insert(objectToSave, collectionName);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[insert] tenantId:{},collectionName:{}",tenantId,collectionName);
		}
		publishRefreshCacheEvent(collectionName, null);
		return entity;
	}

	@Override
	public <T> Collection<T> insert(Collection<? extends T> batchToSave, Class<?> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).insert(batchToSave, entityClass);
	}

	@Override
	public <T> Collection<T> insert(Collection<? extends T> batchToSave, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		Collection<T> result = getRealMongoTemplate(tenantId).insert(batchToSave, collectionName);
		publishRefreshCacheEvent(collectionName, null);
		return result;
	}

	@Override
	public <T> Collection<T> insertAll(Collection<? extends T> objectsToSave) {
		String tenantId = TenantHolder.getTenantId();
		String collectionName = getClassCollectionName(objectsToSave.toArray()[0].getClass());
		Collection<T> entities = getRealMongoTemplate(tenantId).insertAll(objectsToSave);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[insertAll] tenantId:{},collectionName:{} -> size:{}",tenantId,collectionName,objectsToSave.size());
		}
		publishRefreshCacheEvent(collectionName, null);
		return entities;
	}

	@Override
	public <T> T save(T objectToSave) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).save(objectToSave);
	}


	@Override
	public <T> T save(T objectToSave, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).save(objectToSave, collectionName);
	}

	
	@Override
	public DeleteResult remove(Object object) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).remove(object);
	}

	@Override
	public DeleteResult remove(Object object, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		DeleteResult result = getRealMongoTemplate(tenantId).remove(object, collectionName);
		return result;
	}

	@Override
	public DeleteResult remove(Query query, Class<?> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		String collectionName = getClassCollectionName(entityClass);
		DeleteResult result = getRealMongoTemplate(tenantId).remove(query, entityClass);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[remove] tenantId:{},collectionName:{},query:{} -> result:{}",tenantId,collectionName,query,result.getDeletedCount());
		}
		publishRefreshCacheEvent(collectionName, query);
		return result;
	}

	@Override
	public DeleteResult remove(Query query, Class<?> entityClass, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		DeleteResult result = getRealMongoTemplate(tenantId).remove(query, entityClass, collectionName);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[remove] tenantId:{},collectionName:{},query:{} -> result:{}",tenantId,collectionName,query,result.getDeletedCount());
		}
		publishRefreshCacheEvent(collectionName, query);
		return result;
	}

	@Override
	public DeleteResult remove(Query query, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		DeleteResult result = getRealMongoTemplate(tenantId).remove(query, collectionName);
		if(logger.isDebugEnabled()){
			logger.debug(">>_mongoTemplate Method[remove] tenantId:{},collectionName:{},query:{} -> result:{}",tenantId,collectionName,query,result.getDeletedCount());
		}
		publishRefreshCacheEvent(collectionName, query);
		return result;
	}


	@Override
	public <T> List<T> findAllAndRemove(Query query, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).findAllAndRemove(query, collectionName);
	}


	@Override
	public <T> List<T> findAllAndRemove(Query query, Class<T> entityClass) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).findAllAndRemove(query, entityClass);
	}

	@Override
	public <T> List<T> findAllAndRemove(Query query, Class<T> entityClass, String collectionName) {
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).findAllAndRemove(query, entityClass, collectionName);
	}

	private static MongoConverter getDefaultMongoConverter(MongoDbFactory factory) {

		DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
		MongoCustomConversions conversions = new MongoCustomConversions(Collections.emptyList());

		MongoMappingContext mappingContext = new MongoMappingContext();
		mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
		mappingContext.afterPropertiesSet();

		MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
		converter.setCustomConversions(conversions);
		converter.afterPropertiesSet();

		return converter;
	}

	@Override
	public MongoConverter getConverter() {
//		String tenantId;
//		try {
//			tenantId = LoginContext.TenantHolder.getTenantId();
//		} catch (Exception e) {
//			//TODO
//			tenantId = realTenantTemplateMappings.keySet().stream().findFirst().get();
//		}
//		return getRealMongoTemplate(tenantId).getConverter();
		String tenantId = TenantHolder.getTenantId();
		return getRealMongoTemplate(tenantId).getConverter();
	}

	@Override
	public <T> MongoCollection<Document> createCollection(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MongoCollection<Document> createCollection(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> MongoCollection<Document> createCollection(Class<T> arg0, CollectionOptions arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MongoCollection<Document> createCollection(String arg0, CollectionOptions arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long estimatedCount(String arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <T> T findAndModify(Query arg0, UpdateDefinition arg1, Class<T> arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T findAndModify(Query arg0, UpdateDefinition arg1, Class<T> arg2, String arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T findAndModify(Query arg0, UpdateDefinition arg1, FindAndModifyOptions arg2, Class<T> arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T findAndModify(Query arg0, UpdateDefinition arg1, FindAndModifyOptions arg2, Class<T> arg3,
			String arg4) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MongoCollection<Document> getCollection(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateResult updateFirst(Query arg0, UpdateDefinition arg1, Class<?> arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateResult updateFirst(Query arg0, UpdateDefinition arg1, String arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateResult updateFirst(Query arg0, UpdateDefinition arg1, Class<?> arg2, String arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateResult updateMulti(Query arg0, UpdateDefinition arg1, Class<?> arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateResult updateMulti(Query arg0, UpdateDefinition arg1, String arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateResult updateMulti(Query arg0, UpdateDefinition arg1, Class<?> arg2, String arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateResult upsert(Query arg0, UpdateDefinition arg1, Class<?> arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateResult upsert(Query arg0, UpdateDefinition arg1, String arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateResult upsert(Query arg0, UpdateDefinition arg1, Class<?> arg2, String arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MongoOperations withSession(com.mongodb.client.ClientSession arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
