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
package org.dromara.mendmix.mybatis.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.util.StringConverter;
import org.dromara.mendmix.mybatis.core.BaseEntity;
import org.dromara.mendmix.mybatis.crud.annotation.FuzzyMatch;
import org.dromara.mendmix.mybatis.plugin.autofield.annotation.CreatedAt;
import org.dromara.mendmix.mybatis.plugin.autofield.annotation.CreatedBy;
import org.dromara.mendmix.mybatis.plugin.autofield.annotation.UpdatedAt;
import org.dromara.mendmix.mybatis.plugin.autofield.annotation.UpdatedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataHelper {

	private static final Logger log = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");
	
    /**
     * 缓存TableMapper
     */
    private final static Map<Class<?>, EntityMetadata> tableMapperCache = new HashMap<Class<?>, EntityMetadata>();
    private final static Map<String, Map<String, Field>> entityFieldMappings = new HashMap<>();
    private final static Map<String, List<ColumnMetadata>> tableColumnMappings = new HashMap<>();

    /**
     * 由传入的实体的class构建TableMapper对象，构建好的对象存入缓存中，以后使用时直接从缓存中获取
     * 
     * @param entityClass
     * @return TableMapper
     */
    public static EntityMetadata getEntityMapper(Class<?> entityClass) {
        EntityMetadata entityMapper = tableMapperCache.get(entityClass);
        if (entityMapper != null) {
            return entityMapper;
        }
        synchronized (entityClass) {
            TableMetadata tableMapper = getTableMapper(entityClass);
            if(tableMapper == null)return null;
            //获取实体ID泛型
            Class<?> idClass = getIdClass(entityClass);

            // 获取实体字段列表
            List<Field> fields = getAllField(entityClass);
            // 全部列
            Set<ColumnMetadata> columnMapperSet = new HashSet<ColumnMetadata>();
            // 主键
            ColumnMetadata idColumn = null;
            GenerationType idStrategy = null;

            Map<String, Field> map = new HashMap<>();
            for (Field field : fields) {

                // 排除字段
                if (field.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                ColumnMetadata columnMapper = new ColumnMetadata();

                // 数据库字段名
                String columnName = null;
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    columnName = column.name();
                    columnMapper.setInsertable(column.insertable());
                    columnMapper.setUpdatable(column.updatable());
                }
               //乐观锁
                columnMapper.setVersionField(field.isAnnotationPresent(Version.class));
                //审计字段
                columnMapper.setCreatedByField(field.isAnnotationPresent(CreatedBy.class));
                columnMapper.setCreatedAtField(field.isAnnotationPresent(CreatedAt.class));
                columnMapper.setUpdatedAtField(field.isAnnotationPresent(UpdatedAt.class));
                columnMapper.setUpdatedByField(field.isAnnotationPresent(UpdatedBy.class));
                columnMapper.setFuzzyMatch(field.isAnnotationPresent(FuzzyMatch.class));
                //创建字段不能修改
                if(columnMapper.isUpdatable() && (columnMapper.isCreatedAtField() || columnMapper.isCreatedByField())) {
                	columnMapper.setUpdatable(false);
                }
                // 如果为空，使用属性名并替换为下划线风格
                if (columnName == null || columnName.equals("")) {
                    columnName = StringConverter.toUnderlineCase(field.getName());
                }

                columnMapper.setProperty(field.getName());
                columnMapper.setColumn(columnName);
                columnMapper.setJavaType(field.getType());

                // 是否主键
                if(field.isAnnotationPresent(Id.class)){                	
                	columnMapper.setId(true);
                	if(field.isAnnotationPresent(GeneratedValue.class)){ 
                		idStrategy = field.getAnnotation(GeneratedValue.class).strategy();
                	}
                	idColumn = columnMapper;
                }
                // 添加到所有字段映射信息
                columnMapperSet.add(columnMapper);
                //
                field.setAccessible(true);
                map.put(field.getName(), field);
            }
            
            entityFieldMappings.put(tableMapper.getName().toLowerCase(), map);
            
            if (columnMapperSet.size() <= 0) {
                throw new RuntimeException("实体" + entityClass.getName() + "不存在映射字段");
            }
            if (idColumn == null) {
                throw new RuntimeException("实体" + entityClass.getName() + "不存在主键");
            }

            // 解析实体映射信息
            entityMapper = new EntityMetadata();
            entityMapper.setTableMapper(tableMapper);
            entityMapper.setColumnsMapper(columnMapperSet);
            entityMapper.setIdClass(idClass);
            entityMapper.setIdColumn(idColumn);
            entityMapper.setIdStrategy(idStrategy);

            tableMapperCache.put(entityClass, entityMapper);
            //
            tableColumnMappings.put(tableMapper.getName(), new ArrayList<>(columnMapperSet));
            return entityMapper;
        }
    }
    
    public static List<ColumnMetadata> getTableColumnMappers(String tableName){
    	return tableColumnMappings.get(tableName);
    }
    
    public static boolean hasTableColumn(String tableName,String column){
    	if(!tableColumnMappings.containsKey(tableName))return false;
    	return tableColumnMappings.get(tableName).stream().anyMatch(o -> o.getColumn().equals(column));
    }

    /**
     * 获取实体的ID泛型
     * @param entityClass
     * @return
     */
    private static Class<?> getIdClass(Class<?> entityClass) {
        Type[] genTypes = entityClass.getGenericInterfaces();

        for (int i = 0; i < genTypes.length; i++) {
            Type genType = genTypes[i];
            String s1 = genType.getClass().getName();
            String s2 = BaseEntity.class.getName();
            if (s1.startsWith(s2)) {
                Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
                // ID真实的class
                Class<?> mapperClass = (Class<?>) params[0];
                return mapperClass;
            }
        }

        return null;
    }

    // 获取实体Table映射信息
    private static TableMetadata getTableMapper(Class<?> entityClass) {
        // 表名
        TableMetadata tableMapper = new TableMetadata();
        String tableName = null;
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            if (StringUtils.isNotBlank(table.name())) {
                tableName = table.name();
            } else {
                tableName = StringConverter.toUnderlineCase(entityClass.getSimpleName());
            }
        }

        if (tableName == null || tableName.equals("")) {
        	log.error(">>>>>实体" + entityClass.getName() + "不存在'Table'注解");
            return null;
        }

        tableMapper.setName(tableName);
        return tableMapper;
    }



    /**
     * 获取全部的Field
     * 
     * @param entityClass
     * @param fieldList
     * @return
     */
    private static List<Field> getAllField(Class<?> entityClass) {
        return getAllField(entityClass, null);
    }

    /**
     * 获取全部的Field
     *
     * @param entityClass
     * @param fieldList
     * @return
     */
    private static List<Field> getAllField(Class<?> entityClass, List<Field> fieldList) {
        if (fieldList == null) {
            fieldList = new ArrayList<Field>();
        }
        if (entityClass.equals(Object.class)) {
            return fieldList;
        }
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            // 排除静态字段
            if (!Modifier.isStatic(field.getModifiers())) {
                fieldList.add(field);
            }
        }
        Class<?> superClass = entityClass.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)
                && (!Map.class.isAssignableFrom(superClass) && !Collection.class.isAssignableFrom(superClass))) {
            return getAllField(entityClass.getSuperclass(), fieldList);
        }
        return fieldList;
    }
    
    public static Field getEntityField(String tableName,String fieldName){
    	return entityFieldMappings.get(tableName.toLowerCase()).get(fieldName);
    }
}
