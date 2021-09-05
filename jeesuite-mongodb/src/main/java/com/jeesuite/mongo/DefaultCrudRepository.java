package com.jeesuite.mongo;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.jeesuite.common.ThreadLocalContext;
import com.jeesuite.common.model.KeyValues;
import com.jeesuite.common.model.OrderBy;
import com.jeesuite.common.model.Page;
import com.jeesuite.common.model.PageParams;
import com.jeesuite.common.util.ResourceUtils;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;


/**
 * 
 * <br>
 * Class Name   : DefaultCrudRepository
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月30日
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class DefaultCrudRepository<T extends BaseObject> {

	public static final String ORIGIN_PARAM_PREFIX = "~";
	public static final String DATA_PROFILE_IGNORE = "dataprofileIgnore"; //忽略数据权限标记
	public static final String DATA_PROFILE_KEY = "dataprofileKey"; //自定义数据权限字段的集合名（默认就是表名）
	public static final String SHOP_ID_IGNORE = "shopIdIgnore"; //忽略shopId标记
	protected static String[] createTimeRangeQueryFields = {"startTime","endTime"};
	protected static List<String> fuzzyQueryFields = Arrays.asList("name","title");
	private static Map<String, List<Field>> updateableFieldMapping = new HashMap<>();
	private static Map<String, List<String>> fieldNameMapping = new HashMap<>();
	private static Map<String, Map<String,String>> fieldNamePrefixMapping = new HashMap<>();
	private static Map<String,String> multiValueFieldMapping = new HashMap<>();
	
	private static List<String> dataprofileIgnoreList = Arrays.asList(ResourceUtils.getProperty("dataprofile.ignore.list", "").split(","));

	private String collectionName;
	@Autowired
	protected MultiTenantMongoTemplate mongoTemplate;
	
	static {
		//自定义数据权限字段
		Properties properties = ResourceUtils.getAllProperties("dataprofile.spec.fields");
		properties.forEach( (k,v)->{
			String key = k.toString().split("\\[|\\]")[1];
			String[] fields = v.toString().split(";|,");
			List<String> fieldList = new ArrayList<>(fields.length);
			Map<String,String> prefixMapping = new HashMap<>();
            for (String field : fields) {
				if(field.contains(".")) {
					String[] tmpArr = StringUtils.split(field, ".");
					fieldList.add(tmpArr[1]);
					prefixMapping.put(tmpArr[1], tmpArr[0] + ".");
				}else {
					fieldList.add(field);
				}
			}
			fieldNameMapping.put(key, fieldList);
			fieldNamePrefixMapping.put(key, prefixMapping);
		} );
		//
		if(ResourceUtils.containsProperty("multiValue.fields.mapping")) {			
			String[] values = ResourceUtils.getProperty("multiValue.fields.mapping").split(",|;");
			String[] tmpArr;
			for (String v : values) {
				tmpArr = v.split(":");
				multiValueFieldMapping.put(tmpArr[0], tmpArr[1]);
			}
		}
	}
	
	
	public String insert(T entity) {
		String tenantId = TenantHolder.getTenantId();
		if(StringUtils.isNotBlank(tenantId)){
			entity.setTenantId(tenantId);
		}
		entity.initOperateLog(true);
		mongoTemplate.insert(entity);
		return entity.getId();
	}
	
	public void insertList(Collection<T> entities) {
		String tenantId = TenantHolder.getTenantId();
		for (T entity : entities) {
			entity.initOperateLog(true);
			if(StringUtils.isNotBlank(tenantId)){
				entity.setTenantId(tenantId);
			}
		}
		mongoTemplate.insertAll(entities);
	}
	
	public boolean updateById(T entity) {
		entity.initOperateLog(false);
		Query query = new Query(Criteria.where(CommonFields.ID_FIELD_NAME).is(entity.getId()));
		Update update = new Update();
		
		Object value;
		List<Field> fields = getUpdateableFields(entity.getClass());
		for (Field field : fields) {
			try {
				value = field.get(entity);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if(value != null){					
				update.set(field.getName(), value);
			}
		}
		UpdateResult result = mongoTemplate.updateFirst(query, update, entityClass());
		return result.getModifiedCount() > 0;
	}
	
	public <E extends T> E findById(String id,Class<E> entityClass){
		return (E) mongoTemplate.findById(id, entityClass);
	}
	
	public  T findById(String id){
		return (T) mongoTemplate.findById(id, entityClass());
		
	}
	
	public  Map<String,Object> findMapById(String id){
		Map map = mongoTemplate.findById(id, Map.class, collectionName());
		rebuildObjectIdValue(map);
		return map;
	}
	
	public List<T> findByIds(List<String> ids){
		if(ids.size() == 0) {
			return new ArrayList<>(0);
		}
		if(ids.size() == 1) {
			T entity = findById(ids.get(0));
			if(entity == null) {				
				return new ArrayList<>(0);
			}else {
				return Arrays.asList(entity);
			}
		}
		Query query = new Query(Criteria.where(CommonFields.ID_FIELD_NAME).in(ids));
		return (List<T>) mongoTemplate.find(query,entityClass());
	}
	
	public boolean removeById(String id) {
		Query query = new Query(Criteria.where(CommonFields.ID_FIELD_NAME).is(id));
		DeleteResult result = mongoTemplate.remove(query, entityClass());
		return result.getDeletedCount() > 0;
	}
	
	public T getFieldValueById(String id,String...fields){
		Query query = new Query(Criteria.where(CommonFields.ID_FIELD_NAME).is(id));
		for (String field : fields) {
			query.fields().include(field);
		}
		return (T) mongoTemplate.findOne(query, entityClass());
	}
	
	public List<T> findList(Map<String, Object> queryParam){
		Query query = buildQueryObject(queryParam == null ? new HashMap<>(2) : queryParam);
		if(query == null)return new ArrayList<>(0);
		
		query.with(Sort.by(Sort.Order.desc(CommonFields.UPDATE_AT_FIELD_NAME)));
		return (List<T>) mongoTemplate.find(query, entityClass());
	}
	
	public Page<T> pageQuery(PageParams pageParam,Map<String, Object> queryParam,OrderBy orderBy){
		
		Query query = buildQueryObject(queryParam == null ? new HashMap<>(2) : queryParam);
		if(query == null)return new Page<>(pageParam, 0, new ArrayList<>(0));
		
		long total = mongoTemplate.count(query, entityClass());
		
		query.skip(pageParam.offset()).limit(pageParam.getPageSize());
		
		Order updateAtOrder = Sort.Order.desc(CommonFields.UPDATE_AT_FIELD_NAME);
		if(orderBy != null){
			Order order;
			if("DESC".equalsIgnoreCase(orderBy.getSortType())){
				order = Sort.Order.desc(orderBy.getField());
			}else{
				order = Sort.Order.asc(orderBy.getField());
			}
			query.with(Sort.by(order,updateAtOrder));
		}else{
			query.with(Sort.by(updateAtOrder));
		}
		
		if(pageExcludeFields() != null){
			for (String fieldName : pageExcludeFields()) {
				query.fields().exclude(fieldName);
			}
		}
		
		List<T> datas = (List<T>) mongoTemplate.find(query, entityClass());
		
		return new Page<>(pageParam , total, new ArrayList<>(datas));
	}

	
	
	protected List<String> getFieldNames(String profileKey) {
		if(profileKey == null) {
			profileKey = collectionName();
		}
		if(fieldNameMapping.containsKey(profileKey)) {
			return fieldNameMapping.get(profileKey);
		}
		synchronized (fieldNameMapping) {
			if(fieldNameMapping.containsKey(profileKey)) {
				return fieldNameMapping.get(profileKey);
			}
			if(fieldNameMapping.containsKey(collectionName())) {
				return new ArrayList<>(0);
			}
			Field[] fields = FieldUtils.getAllFields(entityClass());
			fieldNameMapping.put(profileKey, new ArrayList<>());
			for (Field field : fields) {
				fieldNameMapping.get(profileKey).add(field.getName());
			}
		}
		return fieldNameMapping.get(profileKey);
	}

	private List<Field> getUpdateableFields(Class<?> clazz) {
		List<Field> list = updateableFieldMapping.get(clazz.getName());
		if(list != null)return list;
		synchronized (DefaultCrudRepository.class) {
			list = updateableFieldMapping.get(clazz.getName());
			if(list != null)return list;
			list = new ArrayList<>();
			Field[] fields = FieldUtils.getAllFields(clazz);
			for (Field field : fields) {
				if (!field.isAnnotationPresent(NonUpdateable.class)) {
					field.setAccessible(true);
					list.add(field);
				}
			}
			updateableFieldMapping.put(clazz.getName(), list);
		}
		return list;
	}
	
	public String insert(Map<String, Object> data) {
		BaseObject.resolveInsertCommonFields(data);
		mongoTemplate.insert(data, collectionName());
		String id = data.remove(CommonFields.ID_FIELD_NAME).toString();
		data.put(CommonFields.ID_FIELD_NAME_ALIAS, id);
		return id;
	}
	
	public void insertList(List<Map<String, Object>> datas) {
		for (Map<String, Object> map : datas) {
			BaseObject.resolveInsertCommonFields(map);
		}
		mongoTemplate.insert(datas, collectionName());
	}
	
	
	public boolean updateById(UpdateParam param) {
		return updateById(param.getParams());
	}
	
	public boolean updateById(Map<String, Object> data) {
		String id;
		if(data.containsKey(CommonFields.ID_FIELD_NAME)){
			id = data.remove(CommonFields.ID_FIELD_NAME).toString();
		}else{
			id = data.remove(CommonFields.ID_FIELD_NAME_ALIAS).toString();
		}
		
		BaseObject.resolveUpdateCommonFields(data);
		Query query = new Query(Criteria.where(CommonFields.ID_FIELD_NAME).is(id));
		Update update = new Update();
		for (String fieldName : data.keySet()) {			
			update.set(fieldName, data.get(fieldName));
		}
		UpdateResult result = mongoTemplate.updateFirst(query, update, collectionName());
		
		return result.getModifiedCount() > 0;
	}
	
	public  List<Map> findMapByIds(List<String> ids){
		Query query = new Query(Criteria.where(CommonFields.ID_FIELD_NAME).in(ids));
		List<Map> result = mongoTemplate.find(query, Map.class, collectionName());
		
		for (Map map : result) {
			rebuildObjectIdValue(map);
		}
		
		return result;
	}
	
	public Page<Map> pageQueryMap(PageParams pageParam,Map<String, Object> queryParam,OrderBy orderBy){
		
		Query query = buildQueryObject(queryParam == null ? new HashMap<>(2) : queryParam);
		if(query == null)return new Page<>(pageParam, 0, new ArrayList<>(0));

		query.skip(pageParam.offset()).limit(pageParam.getPageSize());
//		Pageable pageable = PageRequest.of(pageParam.getPageNo(), pageParam.getPageSize());
//        query.with(pageable);
		
		long total = mongoTemplate.count(query, collectionName());
		
		Order updateAtOrder = Sort.Order.desc(CommonFields.UPDATE_AT_FIELD_NAME);
		if(orderBy != null){
			Order order;
			if("DESC".equalsIgnoreCase(orderBy.getSortType())){
				order = Sort.Order.desc(orderBy.getField());
			}else{
				order = Sort.Order.asc(orderBy.getField());
			}
			query.with(Sort.by(order,updateAtOrder));
		}else{
			query.with(Sort.by(updateAtOrder));
		}
		
		if(pageExcludeFields() != null){
			for (String fieldName : pageExcludeFields()) {
				query.fields().exclude(fieldName);
			}
		}
		
		List<Map> datas = mongoTemplate.find(query, Map.class, collectionName());
		
		//ObjectId id; 
		for (Map map : datas) {
			rebuildObjectIdValue(map);
		}
		
		return new Page<>(pageParam , total, new ArrayList<>(datas));
	}
	
	public String collectionName(){
		if(collectionName != null)return collectionName;
		Class<T> clazz = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		collectionName = clazz.getAnnotation(Document.class).collection();
		return collectionName;
	}
	
	protected void rebuildObjectIdValue(Map<String,Object> map) {
		if(map == null || map.isEmpty())return;
		Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
		Entry<String, Object> entry;
		while(iterator.hasNext()){
			entry = iterator.next();
			if(entry.getValue() instanceof ObjectId){
				map.put(entry.getKey(), entry.getValue().toString());
			}
		}
		String id = map.remove(CommonFields.ID_FIELD_NAME).toString();
		map.put(CommonFields.ID_FIELD_NAME_ALIAS, id);
	}
	
	/**
	 * @param queryParam
	 * @return
	 */
	protected Query buildQueryObject(Map<String, Object> queryParam) {
		Query query = new Query();
		//数据权限
		if(!buildDataProfileCriteria(queryParam,query, null)) {
			return null;
		}
		
		if(queryParam.containsKey(createTimeRangeQueryFields[0]) 
				&& !queryParam.containsKey(createTimeRangeQueryFields[1])){
			queryParam.put(createTimeRangeQueryFields[1], new Date());
		}
		queryParam.forEach( (k,v) -> {
			if(v != null && !k.startsWith(ORIGIN_PARAM_PREFIX) && StringUtils.isNotBlank(v.toString())) {
				if(v instanceof Criteria){
					query.addCriteria((Criteria) v);
				}else{	
					if(fuzzyQueryFields.contains(k) && StringUtils.isNotBlank(queryParam.get(k).toString())){
						Pattern pattern=Pattern.compile(queryParam.remove(k).toString(), Pattern.CASE_INSENSITIVE);
						query.addCriteria(Criteria.where(k).regex(pattern));
					}else if(createTimeRangeQueryFields[0].equals(k)){
						Object endTime = queryParam.get(createTimeRangeQueryFields[1]);
						query.addCriteria(Criteria.where(CommonFields.CREATE_AT_FIELD_NAME).gte(v).lte(endTime));
					}else if(!createTimeRangeQueryFields[1].equals(k)){
						query.addCriteria(Criteria.where(k).is(v));
					}else{
					}			
				}
			}
		});
		
		query.addCriteria(Criteria.where(CommonFields.DELETED_FIELD_NAME).is(false));
		
		return query;
	}
	
	/**
	 * @param baseCriteria
	 * @param queryParam
	 * @return
	 */
	protected Criteria mergeCriterias(Criteria baseCriteria, Map<String, Object> queryParam) {
		List<Criteria> tmpCriterias = null;
		Object value;
		for (String key : queryParam.keySet()) {
			value = queryParam.get(key);
			if(value == null || key.startsWith(ORIGIN_PARAM_PREFIX) || StringUtils.isBlank(value.toString())){
				continue;
			}
			if(value instanceof Criteria){
				if(tmpCriterias == null){
					tmpCriterias = new ArrayList<>(2);
				}
				tmpCriterias.add((Criteria) value);
			}else{					
				baseCriteria.and(key).is(value);
			}
		}
		
		if(tmpCriterias != null){
			tmpCriterias.add(baseCriteria);
			baseCriteria = new Criteria().andOperator(tmpCriterias.toArray(new Criteria[tmpCriterias.size()]));
		}
		return baseCriteria;
	}
	
	/**
	 * 数据权限条件
	 * @param query
	 * @param criteria
	 * @return
	 */
	protected boolean buildDataProfileCriteria(Map<String, Object> queryParam,Query query, Criteria criteria) {
		
		String tenantId = TenantHolder.getTenantId();
		if(tenantId != null){
			if(query != null) {					
				query.addCriteria(Criteria.where(CommonFields.SHOP_ID_FIELD_NAME).is(tenantId));
			}else {
				criteria.and(CommonFields.SHOP_ID_FIELD_NAME).is(tenantId);
			}
		}
		if(queryParam.remove(DATA_PROFILE_IGNORE) != null)return true;
		if(dataprofileIgnoreList.contains(collectionName()))return true;
		// 数据权限
		String profileKey = Objects.toString(queryParam.remove(DATA_PROFILE_KEY), null);
		List<KeyValues> dataProfiles = null;
		if (dataProfiles != null) {
			//
			List<String> fieldNames = getFieldNames(profileKey);
			String prefix;
			String fieldName;
			List<String> values;
			for (KeyValues keyValues : dataProfiles) {
				prefix = null;
				fieldName = keyValues.getKey();
				if (!fieldNames.contains(fieldName))
					continue;
				values = keyValues.getValues();
				// 未分配该字段数据权限
				if (values.isEmpty()) {
					return false;
				}
				
				if(profileKey != null && fieldNamePrefixMapping.containsKey(profileKey)) {					
					prefix = fieldNamePrefixMapping.get(profileKey).get(fieldName);
				}
				
				if(prefix != null) {
					fieldName = prefix + fieldName;
				}
				//查询条件如果包含改字段
				if(queryParam.containsKey(fieldName)) {
					Object originValue = queryParam.remove(ORIGIN_PARAM_PREFIX + keyValues.getKey());
					if(originValue == null) {
						originValue = queryParam.remove(fieldName);
					}
					if(originValue instanceof List) {
						List<String> originValues = (List<String>) originValue;
						if(!originValues.isEmpty()) {
							values = values.stream().filter(o -> originValues.contains(o)).collect(Collectors.toList());
							if(values.isEmpty())return false;
						}
					}else {
						if(!values.contains(originValue)) {
							return false;
						}else {
							values = Arrays.asList(originValue.toString());
						}
					}
					//
					queryParam.remove(fieldName);
				}
                
				if(multiValueFieldMapping.containsKey( keyValues.getKey())) {
                	fieldName = multiValueFieldMapping.get(keyValues.getKey());
                	if(prefix != null)fieldName = prefix + fieldName;
                	
                	Criteria criteria1 = Criteria.where("$in").is(values);
                	if(query != null) {						
						query.addCriteria(Criteria.where(fieldName).elemMatch(criteria1));
					}else {
						criteria.and(fieldName).elemMatch(criteria1);
					}
				}else {
					if (values.size() == 1) {
						if(query != null) {						
							query.addCriteria(Criteria.where(fieldName).is(values.get(0)));
						}else {
							criteria.and(fieldName).is(values.get(0));
						}
					} else {
						if(query != null) {							
							query.addCriteria(Criteria.where(fieldName).in(values));
						}else {	
							criteria.and(fieldName).in(values);
						}
					}
				}
			}
		}
		
		return true;
	}
	
	
	public String[] pageExcludeFields(){
		return null;
	}
	
	
	public abstract Class<? extends BaseObject> entityClass();
	
}
