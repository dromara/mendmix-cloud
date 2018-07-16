package com.jeesuite.mybatis.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.w3c.dom.NodeList;

import com.jeesuite.spring.InstanceFactory;

/**
 * mybatismapper数据库字段与实体字段映射关系转换工具
 * @author jwww
 * @date 2015年5月7日上午11:30:42
 * @description <br>
 * Copyright (c) 2015, vakinge@gmail.com.
 */
public class MybatisMapperParser {

	
	private static final Logger log = LoggerFactory.getLogger(MybatisMapperParser.class);
	
	private static Map<String, Map<String, String>> caches = new HashMap<String, Map<String,String>>();
	
	private static Map<String, List<MapResultItem>> entityRalateItems = new HashMap<String, List<MapResultItem>>();
	
	private static Map<String, List<MapResultItem>> tableRalateItems = new HashMap<String, List<MapResultItem>>();
	
	private static Map<String, List<String>> namespaceRalateColumns = new HashMap<String, List<String>>();
	
	private static List<EntityInfo> entityInfos = new ArrayList<>();
	
	private static Map<String,EntityInfo> mapperRalateEntitys = new HashMap<>();
	
	private static String mapperLocations;

	public static void setMapperLocations(String mapperLocations){
		MybatisMapperParser.mapperLocations = mapperLocations;
	}
	
	public static List<EntityInfo> getEntityInfos() {
		doParse();
		return entityInfos;
	}
	
	public static EntityInfo getEntityInfoByMapper(String mapperName){
		doParse();
		return mapperRalateEntitys.get(mapperName);
	}
	
	public static boolean entityHasProperty(Class<?> entityClass,String propName){
		return property2ColumnName(entityClass, propName) != null;
	}

	public static String columnToPropName(Class<?> entityClass,String columnName){
		doParse();
		if(caches.containsKey(entityClass.getName())){
			return caches.get(entityClass.getName()).get(columnName);
		}
		return null;
	}
	
	public static String property2ColumnName(Class<?> entityClass,String propName){
		doParse();
		Map<String, String> map = caches.get(entityClass.getName());
		if(map != null){
			for (String columnName : map.keySet()) {
				if(propName.equals(map.get(columnName)))return columnName;
			}
		}
		return null;
	}
	
	public static boolean tableHasColumn(String namespace,String columnName){
		List<String> list = namespaceRalateColumns.get(namespace);
		return list != null && list.contains(columnName.toLowerCase());
	}
	
	private synchronized static void doParse(){
		if(!caches.isEmpty())return;
		try {
			ResourceLoader resourceLoader = InstanceFactory.getInstance(ResourceLoader.class);
			if(resourceLoader == null)resourceLoader = new DefaultResourceLoader();
			Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources(mapperLocations);
			for (Resource resource : resources) {
				log.info(">begin parse mapper file:" + resource);
				parseMapperFile(resource.getFilename(),resource.getInputStream());
			}
		} catch (Exception e) {
			log.error("解析mapper文件异常", e);	
			throw new RuntimeException("解析mapper文件异常");
		}
	}
	
	
	private static void parseMapperFile(String fileName,InputStream inputStream) throws Exception {
		
		XPathParser parser = new XPathParser(inputStream,true, null, new XMLMapperEntityResolver());

		XNode evalNode = parser.evalNode("/mapper");
		
		String mapperClass = evalNode.getStringAttribute("namespace");
		String entityClass = null;
		EntityInfo entityInfo = null;
		
		Map<String, String> includes = new HashMap<>();
		
		List<XNode> children = evalNode.getChildren();
		for (XNode xNode : children) {
			if("sql".equalsIgnoreCase(xNode.getName())){
				includes.put(xNode.getStringAttribute("id"), xNode.getStringBody());
				continue;
			}
			if(!"resultMap".equals(xNode.getName()) )continue;
			if(!"BaseResultMap".equals(xNode.getStringAttribute("id")) )continue;
			
			entityClass = xNode.getStringAttribute("type");
			entityInfo = new EntityInfo(mapperClass, entityClass);
			
			if(entityInfo.getErrorMsg() != null){				
				log.warn("==================\n>>{},skip！！！！\n===============",entityInfo.getErrorMsg());
				continue;
			}
			entityInfos.add(entityInfo);
			mapperRalateEntitys.put(mapperClass, entityInfo);
			//
			List<XNode> resultNodes = xNode.getChildren();
			for (XNode xNode2 : resultNodes) {
				parseResultNode(entityInfo,xNode2);
			}
		}
		
		if(entityInfo.getErrorMsg() != null){
			return;
		}
		for (XNode xNode : children) {
			if ("select|insert|update|delete".contains(xNode.getName().toLowerCase())) {
				String sql = parseSql(fileName,xNode,includes);
				entityInfo.addSql(xNode.getStringAttribute("id"), sql);
			}
		}
		
		inputStream.close();
	}
	
	private static void parseResultNode(EntityInfo entityInfo,XNode node){
		MapResultItem resultItem = new MapResultItem();
		resultItem.setEntityName(entityInfo.getEntityClass().getName());
		resultItem.setTableName(entityInfo.getTableName());
		resultItem.setColumnName(node.getStringAttribute("column"));
		resultItem.setPrimaryKey("id".equals(node.getName().toLowerCase()));
		resultItem.setPropertyName(node.getStringAttribute("property"));
		resultItem.setType(node.getStringAttribute("jdbcType"));
		
		//
		Map<String,String> resultRalate = caches.get(resultItem.getEntityName());
		if(resultRalate == null){
			resultRalate = new HashMap<String, String>();
			caches.put(resultItem.getEntityName(), resultRalate);
		}
		resultRalate.put(resultItem.getColumnName(), resultItem.getPropertyName());
		
		//
		List<MapResultItem> list = entityRalateItems.get(resultItem.getEntityName());
		if(list == null){
			list = new ArrayList<>();
			entityRalateItems.put(resultItem.getEntityName(), list);
		}
		list.add(resultItem);
		
		//
		List<MapResultItem> list2 = tableRalateItems.get(resultItem.getEntityName());
		if(list2 == null){
			list2 = new ArrayList<>();
			tableRalateItems.put(resultItem.getTableName(), list2);
		}
		list2.add(resultItem);
		
		//
		List<String> tmplist3 = namespaceRalateColumns.get(entityInfo.getMapperClass().getName());
		if(tmplist3 == null){
			tmplist3 = new ArrayList<>();
			namespaceRalateColumns.put(entityInfo.getMapperClass().getName(), tmplist3);
		}
		tmplist3.add(resultItem.getColumnName());
		
	}
	
	private static String parseSql(String fileName,XNode node,Map<String, String> includeContents) {
	    StringBuilder sql = new StringBuilder();
	    NodeList children = node.getNode().getChildNodes();
	    for (int i = 0; i < children.getLength(); i++) {
	      XNode child = node.newXNode(children.item(i));
	      String data = null;
	      if("#text".equals(child.getName())){
	    	  data = child.getStringBody("");
	      }else if("include".equals(child.getName())){
	    	  String refId = child.getStringAttribute("refid");
	    	  data = child.toString();
	    	  if(includeContents.containsKey(refId)){	    		  
	    		  data = data.replaceAll("<\\s?include.*("+refId+").*>", includeContents.get(refId));
	    	  }else{
	    		  log.error(String.format(">>>>>Parse SQL from mapper[%s-%s] error,not found include key:%s", fileName,node.getStringAttribute("id"),refId));
	    	  }
	      }else{
	    	  data = child.toString();
//	    	  if(child.getStringBody().contains(">") || child.getStringBody().contains("<")){
//	    		  data = data.replace(child.getStringBody(), "<![CDATA["+child.getStringBody()+"]]");
//	    	  }
	      }
	      data = data.replaceAll("\\n+|\\t+", "");
	      if(StringUtils.isNotBlank(data)){	    	  
	    	  sql.append(data).append("\t").append("\n");
	      }
	    }
	    // return sql.toString().replaceAll("\\s{2,}", " ");
		return sql.toString();
	  }
	
	public static List<String> listFiles(JarFile jarFile, String extensions) {
		if (jarFile == null || StringUtils.isEmpty(extensions))
			return null;
		
		List<String> files = new ArrayList<String>();
		
		Enumeration<JarEntry> entries = jarFile.entries(); 
        while (entries.hasMoreElements()) {  
        	JarEntry entry = entries.nextElement();
        	String name = entry.getName();
    		
    		if (name.endsWith(extensions)) {
    			files.add(name);
    		}
        } 
        
        return files;
	}
	
	public static void main(String[] args) {
		String sql = "SELECT <include refid=\"base_fields\" /> dd > FROM users where type = #{type} ";
		System.out.println(sql.replaceAll("<\\s?include.*(base_fields).*>", "xxxx"));
	}
}
