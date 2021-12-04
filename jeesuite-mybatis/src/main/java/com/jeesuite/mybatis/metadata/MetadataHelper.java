package com.jeesuite.mybatis.metadata;

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

import com.jeesuite.mybatis.core.BaseEntity;
import com.jeesuite.mybatis.plugin.autofield.annotation.CreatedAt;
import com.jeesuite.mybatis.plugin.autofield.annotation.CreatedBy;
import com.jeesuite.mybatis.plugin.autofield.annotation.UpdatedAt;
import com.jeesuite.mybatis.plugin.autofield.annotation.UpdatedBy;

public class MetadataHelper {

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
     * @return EntityMetadata
     */
    public static EntityMetadata getEntityMapper(Class<?> entityClass) {

    	EntityMetadata entityMeta = tableMapperCache.get(entityClass);
        // 如果存在直接返回
        if (entityMeta != null) {
            return entityMeta;
        }
        synchronized (entityClass) {
            TableMetadata tableMeta = getTableMapper(entityClass);
            //获取实体ID泛型
            Class<?> idClass = getIdClass(entityClass);
            // 获取实体字段列表
            List<Field> fields = getAllField(entityClass);
            // 全部列
            Set<ColumnMetadata> columnMetas = new HashSet<ColumnMetadata>();
            // 主键
            ColumnMetadata idColumn = null;
            GenerationType idStrategy = null;

            Map<String, Field> map = new HashMap<>();
            for (Field field : fields) {

                // 排除字段
                if (field.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                ColumnMetadata columnMeta = new ColumnMetadata();

                // 数据库字段名
                String columnName = null;
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    columnName = column.name();
                    columnMeta.setInsertable(column.insertable());
                    columnMeta.setUpdatable(column.updatable());
                }
               //乐观锁
                columnMeta.setVersionField(field.isAnnotationPresent(Version.class));
                //审计字段
                columnMeta.setCreatedByField(field.isAnnotationPresent(CreatedBy.class));
                columnMeta.setCreatedAtField(field.isAnnotationPresent(CreatedAt.class));
                columnMeta.setUpdatedAtField(field.isAnnotationPresent(UpdatedAt.class));
                columnMeta.setUpdatedByField(field.isAnnotationPresent(UpdatedBy.class));
                //创建字段不能修改
                if(columnMeta.isUpdatable() && (columnMeta.isCreatedAtField() || columnMeta.isCreatedByField())) {
                	columnMeta.setUpdatable(false);
                }
                // 如果为空，使用属性名并替换为下划线风格
                if (columnName == null || columnName.equals("")) {
                    columnName = camelhumpToUnderline(field.getName());
                }

                columnMeta.setProperty(field.getName());
                columnMeta.setColumn(columnName);
                columnMeta.setJavaType(field.getType());

                // 是否主键
                if(field.isAnnotationPresent(Id.class)){                	
                	columnMeta.setId(true);
                	if(field.isAnnotationPresent(GeneratedValue.class)){ 
                		idStrategy = field.getAnnotation(GeneratedValue.class).strategy();
                	}
                	idColumn = columnMeta;
                }
                // 添加到所有字段映射信息
                columnMetas.add(columnMeta);
                //
                field.setAccessible(true);
                map.put(field.getName(), field);
            }
            
            entityFieldMappings.put(tableMeta.getName(), map);
            
            if (columnMetas.size() <= 0) {
                throw new RuntimeException("实体" + entityClass.getName() + "不存在映射字段");
            }
            if (idColumn == null) {
                throw new RuntimeException("实体" + entityClass.getName() + "不存在主键");
            }

            // 解析实体映射信息
            entityMeta = new EntityMetadata();
            entityMeta.setTable(tableMeta);
            entityMeta.setColumns(columnMetas);
            entityMeta.setIdClass(idClass);
            entityMeta.setIdColumn(idColumn);
            entityMeta.setIdStrategy(idStrategy);

            tableMapperCache.put(entityClass, entityMeta);
            //
            tableColumnMappings.put(tableMeta.getName().toLowerCase(), new ArrayList<>(columnMetas));
            return entityMeta;
        }
    }
    
    public static List<ColumnMetadata> getTableColumnMappers(String tableName){
    	return tableColumnMappings.get(tableName.toLowerCase());
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
                tableName = camelhumpToUnderline(entityClass.getSimpleName());
            }
        }

        if (StringUtils.isBlank(tableName)) {
            throw new RuntimeException("实体" + entityClass.getName() + "不存在'Table'注解");
        }

        tableMapper.setName(tableName.toLowerCase());
        return tableMapper;
    }

    /**
     * 将驼峰风格替换为下划线风格
     */
    private static String camelhumpToUnderline(String str) {
        final int size;
        final char[] chars;
        final StringBuilder sb = new StringBuilder((size = (chars = str.toCharArray()).length) * 3 / 2 + 1);
        char c;
        for (int i = 0; i < size; i++) {
            c = chars[i];
            if (isUppercaseAlpha(c)) {
                sb.append('_').append(c);
            } else {
                sb.append(toUpperAscii(c));
            }
        }
        String result = sb.charAt(0) == '_' ? sb.substring(1) : sb.toString();
        return result.toLowerCase();
    }

    private static boolean isUppercaseAlpha(char c) {
        return (c >= 'A') && (c <= 'Z');
    }

    private static char toUpperAscii(char c) {
        if (isUppercaseAlpha(c)) {
            c -= (char) 0x20;
        }
        return c;
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
