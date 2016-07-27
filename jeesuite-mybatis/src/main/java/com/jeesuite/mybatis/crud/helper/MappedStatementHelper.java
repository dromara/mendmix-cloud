//package com.jeesuite.mybatis.crud.helper;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.ibatis.mapping.MappedStatement;
//import org.apache.ibatis.mapping.ResultMap;
//import org.apache.ibatis.mapping.ResultMapping;
//import org.apache.ibatis.mapping.SqlCommandType;
//import org.apache.ibatis.mapping.SqlSource;
//import org.apache.ibatis.reflection.MetaObject;
//import org.apache.ibatis.scripting.defaults.RawSqlSource;
//import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
//import org.apache.ibatis.scripting.xmltags.MixedSqlNode;
//import org.apache.ibatis.scripting.xmltags.SqlNode;
//import org.apache.ibatis.scripting.xmltags.StaticTextSqlNode;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.jeesuite.mybatis.crud.builder.SqlBuilder;
//
//import tk.mybatis.mapper.common.BaseMapper;
//
///**
// * 处理主要逻辑，最关键的一个类
// * 
// * @author LinHaobin
// *
// */
//public class MappedStatementHelper {
//
//	private static Logger log = LoggerFactory.getLogger(MappedStatementHelper.class);
//	private static Map<Class<?>, List<ResultMap>> resultMapsCache = new HashMap<Class<?>, List<ResultMap>>();
//
//	/**
//	 * 缓存skip结果
//	 */
//	private static final Map<String, Boolean> msIdSkip = new HashMap<String, Boolean>();
//
//	/**
//	 * 判断当前的接口方法是否需要进行拦截
//	 *
//	 * @param msId
//	 * @return
//	 */
//	private static boolean isSkip(String msId) {
//		if (msIdSkip.containsKey(msId)) {
//			return true;
//		}
//		msIdSkip.put(msId, true);
//		return false;
//	}
//
//	public static void build(MappedStatement ms) throws Exception {
//		// 不需要拦截的方法直接返回
//		if (MappedStatementHelper.isSkip(ms.getId())) {
//			return;
//		}
//
//		String fullSqlId = ms.getId();
//		String sqlId = fullSqlId.substring(fullSqlId.lastIndexOf(".") + 1);
//
//		// 子类已重写mapper的方法
//		if (MappedStatementHelper.isOverride(ms, sqlId)) {
//			// 替换 @Select 占位符
//			replaceSelectSql(ms);
//			setResultMap(ms);
//			return;
//		}
//
//		// 自动生成sql
//		if (sqlId.equals("getById")) {
//			getById(ms);
//		} else if (sqlId.equals("insert")) {
//			insert(ms);
//		} else if (sqlId.equals("delete")) {
//			delete(ms);
//		} else if (sqlId.equals("update")) {
//			update(ms);
//		} else {
//			// 替换 @Select 占位符
//			replaceSelectSql(ms);
//		}
//
//		// 设置返回ResultMap
//		setResultMap(ms);
//	}
//
//	/**
//	 * 判断在mapper中是否重写xml
//	 * 
//	 * @param ms
//	 * @param methodName
//	 * @return
//	 */
//	private static boolean isOverride(MappedStatement ms, String methodName) {
//		String res = ms.getResource();
//		String suffix = res.substring(res.lastIndexOf(".") + 1);
//		String reg = "^(getById)|(insert)|(delete)|(update)$";
//		return suffix.equals("xml") && methodName.matches(reg);
//	}
//
//	/**
//	 * 设置返回值类型
//	 *
//	 * @param ms
//	 * @param entityClass
//	 */
//	private static void setResultType(MappedStatement ms, Class<?> entityClass) {
//		ResultMap resultMap = ms.getResultMaps().get(0);
//		MetaObject metaObject = forObject(resultMap);
//		metaObject.setValue("type", entityClass);
//	}
//
//	/**
//	 * 重新设置SqlSource
//	 *
//	 * @param ms
//	 * @param sqlSource
//	 */
//	private static void setSqlSource(MappedStatement ms, String sql) {
//		DynamicSqlSource dynamicSqlSource = new DynamicSqlSource(ms.getConfiguration(), generateSqlNode(sql));
//		setSqlSource(ms, dynamicSqlSource);
//	}
//
//	/**
//	 * 重新设置SqlSource
//	 *
//	 * @param ms
//	 * @param sqlSource
//	 */
//	private static void setSqlSource(MappedStatement ms, SqlSource sqlSource) {
//		MetaObject msObject = forObject(ms);
//		msObject.setValue("sqlSource", sqlSource);
//	}
//
//	private static MetaObject forObject(Object object) {
//		return MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY);
//	}
//
//	/**
//	 * 获取Mapper泛型实体类的class
//	 *
//	 * @param ms
//	 * @return
//	 */
//	private static Class<?> getMapperEntityClass(String msId) {
//		try {
//			if (msId.indexOf(".") == -1) {
//				throw new RuntimeException("当前MappedStatement的id=" + msId + ",不符合MappedStatement的规则!");
//			}
//			String mapperClassName = msId.substring(0, msId.lastIndexOf("."));
//
//			// 获取 接口泛型 T的真实类
//			Class<?> mapperClass = Class.forName(mapperClassName);
//			return ReflectUtil.getGenericInterfaceActualType(mapperClass, BaseMapper.class, 0);
//		} catch (Exception e) {
//			throw new RuntimeException("无法获取Mapper<T>泛型类型:" + msId, e);
//		}
//
//	}
//
//	private static SqlNode generateSqlNode(String sql) {
//
//		List<SqlNode> sqlNodes = new ArrayList<SqlNode>();
//		sqlNodes.add(new StaticTextSqlNode(sql));
//		SqlNode sqlNode = new MixedSqlNode(sqlNodes);
//
//		return sqlNode;
//	}
//
//	/**
//	 * 根据主键进行查询
//	 *
//	 * @param ms
//	 */
//	private static void getById(MappedStatement ms) {
//
//		Class<?> entityClass = getMapperEntityClass(ms.getId());
//
//		// 从参数对象里提取注解信息
//		EntityMapper entityMapper = EntityHelper.getEntityMapper(entityClass);
//
//		// 生成sql
//		String sql = SqlBuilder.buildGetByIdSql(entityMapper);
//
//		setSqlSource(ms, sql);
//
//		// 将返回值修改为实体类型
//		setResultType(ms, entityClass);
//	}
//
//	private static void insert(MappedStatement ms) {
//		Class<?> entityClass = getMapperEntityClass(ms.getId());
//
//		// 从参数对象里提取注解信息
//		EntityMapper entityMapper = EntityHelper.getEntityMapper(entityClass);
//
//		// 生成sql
//		String sql = SqlBuilder.buildInsertSql(entityMapper);
//
//		// 将sql设置到MappedStatement
//		setSqlSource(ms, sql);
//
//	}
//
//	private static void delete(MappedStatement ms) {
//		Class<?> entityClass = getMapperEntityClass(ms.getId());
//
//		// 从参数对象里提取注解信息
//		EntityMapper entityMapper = EntityHelper.getEntityMapper(entityClass);
//
//		// 生成sql
//		String sql = SqlBuilder.buildDeleteSql(entityMapper);
//
//		setSqlSource(ms, sql);
//
//	}
//
//	private static void update(MappedStatement ms) {
//		Class<?> entityClass = getMapperEntityClass(ms.getId());
//
//		// 从参数对象里提取注解信息
//		EntityMapper entityMapper = EntityHelper.getEntityMapper(entityClass);
//
//		// 生成sql
//		String sql = SqlBuilder.buildUpdateSql(entityMapper);
//
//		setSqlSource(ms, sql);
//
//	}
//
//	private static void replaceSelectSql(MappedStatement ms) throws ClassNotFoundException {
//		MetaObject msObject = forObject(ms);
//		Object sqlSource = msObject.getValue("sqlSource");
//		if (sqlSource instanceof RawSqlSource) { // 静态sql
//			// 获取node
//			MetaObject node = getRawSqlSourceNode(sqlSource);
//			// 获取sql
//			String sql = getRawSqlSourceSql(node);
//			// 判断是否存在自定义的占位符()
//			if (sql.indexOf(SqlBuilder.PLACEHOLDER) > -1) {
//				sql = replaceSql(ms, SqlBuilder.PLACEHOLDER, sql);
//			} else if (sql.toUpperCase().startsWith(SqlBuilder.FROM)) {
//				sql = replaceSql(ms, null, sql);
//			}
//			// 设置sql
//			setRawSqlSource(node, sql);
//		} else { // 动态sql
//			// 获取node
//			MetaObject node = getDynamicSqlSourceNode(sqlSource);
//			// 获取sql
//			String sql = getDynamicSqlSourceSql(node);
//			// 判断是否存在自定义的占位符()
//			if (sql.indexOf(SqlBuilder.PLACEHOLDER) > -1) {
//				sql = replaceSql(ms, SqlBuilder.PLACEHOLDER, sql);
//			} else if (sql.toUpperCase().startsWith(SqlBuilder.FROM)) {
//				sql = replaceSql(ms, null, sql);
//			}
//			// 设置sql
//			setDynamicSqlSource(node, sql);
//		}
//	}
//
//	private static MetaObject getDynamicSqlSourceNode(Object sqlSource) {
//		MetaObject sqlSourceObject = forObject(sqlSource);
//		List<?> nodes = (List<?>) sqlSourceObject.getValue("rootSqlNode.contents");
//		MetaObject node = forObject(nodes.get(0));
//		return node;
//	}
//
//	private static MetaObject getRawSqlSourceNode(Object sqlSource) {
//		MetaObject sqlSourceObject = forObject(sqlSource);
//		Object sqlObject = sqlSourceObject.getValue("sqlSource");
//		MetaObject node = forObject(sqlObject);
//		return node;
//	}
//
//	private static void setDynamicSqlSource(MetaObject node, String sql) {
//		node.setValue("text", sql);
//	}
//
//	private static String getDynamicSqlSourceSql(MetaObject node) {
//		return (String) node.getValue("text");
//	}
//
//	private static void setRawSqlSource(MetaObject node, String sql) {
//		node.setValue("sql", sql);
//	}
//
//	private static String replaceSql(MappedStatement ms, String placeholder, String sql) {
//
//		Class<?> entityClass = getMapperEntityClass(ms.getId());
//
//		// 从参数对象里提取注解信息
//		EntityMapper entityMapper = EntityHelper.getEntityMapper(entityClass);
//
//		// 生成列字段sql
//		String columSql = SqlBuilder.buildColumSql(entityMapper);
//
//		// 替换占位符
//		if (placeholder == null) {
//			sql = columSql + " " + sql;
//		} else {
//			sql = sql.replaceAll(placeholder, columSql);
//		}
//
//		return sql;
//	}
//
//	private static String getRawSqlSourceSql(MetaObject node) {
//		return (String) node.getValue("sql");
//	}
//
//	/**
//	 * 生成数据库表字段与实体属性映射关系-ResultMap
//	 * 
//	 * @param ms
//	 */
//	private static void setResultMap(MappedStatement ms) {
//		if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
//			return;
//		}
//
//		Class<?> resultType = null;
//
//		// 如果没有设置返回类型，则默认为mapper对应实体
//		if (ms.getResultMaps().size() == 0) {
//
//			resultType = getMapperEntityClass(ms.getId());
//
//		} else {
//			ResultMap resultMap = ms.getResultMaps().get(0);
//
//			// {{id}}-BaseEntity情况是BaseMapper生成的
//			if (!(ms.getId() + "-Object").equals(resultMap.getId()) && !(ms.getId() + "-BaseEntity").equals(resultMap.getId())) {
//				return;
//			}
//
//			// 原来的返回类型
//			resultType = resultMap.getType();
//		}
//
//		// 生成数据库表字段与实体属性映射关系-ResultMap(column 表字段名（_命名方式），property属性名(驼峰命名方式))
//		List<ResultMap> newResultMaps = generateResultMaps(ms, resultType);
//
//		if (newResultMaps == null) {
//			return;
//		}
//
//		MetaObject msObject = forObject(ms);
//		msObject.setValue("resultMaps", newResultMaps);
//	}
//
//	private static List<ResultMap> generateResultMaps(MappedStatement ms, Class<?> resultType) {
//
//		List<ResultMap> resultMaps = resultMapsCache.get(resultType);
//
//		if (resultMaps != null) {
//			return resultMaps;
//		}
//
//		EntityMapper entityMapper = null;
//		try {
//			// 如果没有返回类没有Table注解，不作处理
//			entityMapper = EntityHelper.getEntityMapper(resultType);
//		} catch (Exception e) {
//			log.debug("该返回值不是实体(没有Table注解)");
//			return null;
//		}
//
//		List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
//
//		for (ColumnMapper columnMapper : entityMapper.getColumnsMapper()) {
//			ResultMapping resultMapping = null;
//			// if (columnMapper.getTypeHandler() != null) {
//			// TypeHandler<?> typeHandler =
//			// ms.getConfiguration().getTypeHandlerRegistry().getInstance(columnMapper.getJavaType(),
//			// columnMapper.getTypeHandler());
//			// resultMapping = new ResultMapping.Builder(ms.getConfiguration(),
//			// columnMapper.getProperty(), columnMapper.getColumn(),
//			// typeHandler).build();
//			// } else {
//			resultMapping = new ResultMapping.Builder(ms.getConfiguration(), columnMapper.getProperty(), columnMapper.getColumn(), columnMapper.getJavaType()).build();
//			// }
//			resultMappings.add(resultMapping);
//		}
//
//		String resultMapId = ms.getId().substring(0, ms.getId().lastIndexOf(".") + 1) + "simple_entity_map";
//
//		ResultMap resultMap = new ResultMap.Builder(ms.getConfiguration(), resultMapId, resultType, resultMappings).build();
//
//		resultMaps = new ArrayList<ResultMap>();
//		resultMaps.add(resultMap);
//
//		resultMapsCache.put(resultType, resultMaps);
//
//		return resultMaps;
//	}
//
//}