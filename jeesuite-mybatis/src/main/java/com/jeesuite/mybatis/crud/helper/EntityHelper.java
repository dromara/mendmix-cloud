package com.jeesuite.mybatis.crud.helper;

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

import com.jeesuite.mybatis.core.BaseEntity;

public class EntityHelper {

    /**
     * 缓存TableMapper
     */
    private final static Map<Class<?>, EntityMapper> tableMapperCache = new HashMap<Class<?>, EntityMapper>();
    private final static Map<String, Map<String, Field>> entityFieldMappings = new HashMap<>();

    /**
     * 由传入的实体的class构建TableMapper对象，构建好的对象存入缓存中，以后使用时直接从缓存中获取
     * 
     * @param entityClass
     * @return TableMapper
     */
    public static EntityMapper getEntityMapper(Class<?> entityClass) {

        synchronized (entityClass) {
            // 先从map中获取实体映射信息
            EntityMapper entityMapper = tableMapperCache.get(entityClass);

            // 如果存在直接返回
            if (entityMapper != null) {
                return entityMapper;
            }

            TableMapper tableMapper = getTableMapper(entityClass);

            //获取实体ID泛型
            Class<?> idClass = getIdClass(entityClass);

            // 获取实体字段列表
            List<Field> fields = getAllField(entityClass);
            // 全部列
            Set<ColumnMapper> columnMapperSet = new HashSet<ColumnMapper>();
            // 主键
            ColumnMapper idColumn = null;
            GenerationType idStrategy = null;

            Map<String, Field> map = new HashMap<>();
            for (Field field : fields) {

                // 排除字段
                if (field.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                ColumnMapper columnMapper = new ColumnMapper();

                // 数据库字段名
                String columnName = null;
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    columnName = column.name();
                    columnMapper.setInsertable(column.insertable());
                    columnMapper.setUpdatable(column.updatable());
                }
                // 如果为空，使用属性名并替换为下划线风格
                if (columnName == null || columnName.equals("")) {
                    columnName = camelhumpToUnderline(field.getName());
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
            
            entityFieldMappings.put(tableMapper.getName(), map);
            
            if (columnMapperSet.size() <= 0) {
                throw new RuntimeException("实体" + entityClass.getName() + "不存在映射字段");
            }
            if (idColumn == null) {
                throw new RuntimeException("实体" + entityClass.getName() + "不存在主键");
            }

            // 解析实体映射信息
            entityMapper = new EntityMapper();
            entityMapper.setTableMapper(tableMapper);
            entityMapper.setColumnsMapper(columnMapperSet);
            entityMapper.setIdClass(idClass);
            entityMapper.setIdColumn(idColumn);
            entityMapper.setIdStrategy(idStrategy);

            tableMapperCache.put(entityClass, entityMapper);

            return entityMapper;
        }
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
    private static TableMapper getTableMapper(Class<?> entityClass) {
        // 表名
        TableMapper tableMapper = new TableMapper();
        String tableName = null;
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            if (!table.name().equals("")) {
                tableName = table.name();
            } else {
                tableName = camelhumpToUnderline(entityClass.getSimpleName());
            }
        }

        if (tableName == null || tableName.equals("")) {
            throw new RuntimeException("实体" + entityClass.getName() + "不存在'Table'注解");
        }

        tableMapper.setName(tableName);
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
    	return entityFieldMappings.get(tableName).get(fieldName);
    }
}
