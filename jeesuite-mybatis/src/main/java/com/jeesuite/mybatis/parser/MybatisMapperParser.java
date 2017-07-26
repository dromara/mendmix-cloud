package com.jeesuite.mybatis.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
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
import org.w3c.dom.NodeList;

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
	
	private static String mapperFileSuffix = "Mapper.xml";
	private static String mapperBaseDir;
	public static void setMapperLocations(String mapperLocations){
		if(mapperBaseDir != null)return;
		//classpath:META-INF/mapper/*Mapper.xml
		mapperLocations = mapperLocations.split(":")[1];
		int spitPos = mapperLocations.lastIndexOf("/");
		mapperBaseDir = mapperLocations.substring(0, spitPos);
		mapperFileSuffix = mapperLocations.substring(spitPos + 1).replace("*", "");
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
		if(caches.isEmpty()){
			try {
				URL resource = Thread.currentThread().getContextClassLoader().getResource(mapperBaseDir);
				if (resource != null) {			
					if (resource.getProtocol().equals("file")) {
						File mapperDir = new File(resource.getPath());
						File[] files = mapperDir.listFiles();						
						for (File f : files) {
							if(f.getName().endsWith(mapperFileSuffix)){
								parseMapperFile(new FileInputStream(f));
							}
						}
					} else if (resource.getProtocol().equals("jar")) {
						String jarFilePath = resource.getFile();	
						
						//file:/Users/vakinge/.m2/repository/com/jeesuite/demo/demo-dao/1.0-SNAPSHOT/demo-dao-1.0-SNAPSHOT.jar!/mapper;
						jarFilePath = jarFilePath.split("jar!")[0] + "jar";
						jarFilePath = jarFilePath.substring("file:".length());
						log.info("mapper file in jar:{}",jarFilePath);
						jarFilePath = java.net.URLDecoder.decode(jarFilePath, "UTF-8");

						JarFile jarFile = new JarFile(jarFilePath);
						
						List<String> fileNames = listFiles(jarFile, mapperFileSuffix);
						if (fileNames != null && fileNames.size() > 0) {
							for (String fileName : fileNames) {
								InputStream inputStream = jarFile.getInputStream(jarFile.getJarEntry(fileName));
								parseMapperFile(inputStream);
								try {inputStream.close();} catch (Exception e) {}
							}
						}
						
						jarFile.close();
					} else {
						log.error("mapper dir is in unsurport protocol");
					}															
				} else {
					log.error("can not find mapper dir");
				}				
			} catch (Exception e) {
				log.error("解析mapper文件异常", e);	
				
				throw new RuntimeException("解析mapper文件异常");
			}
		}
	}
	
	private static void parseMapperFile(InputStream inputStream) throws Exception {
		
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
				String sql = parseSql(xNode,includes);
				entityInfo.addSql(xNode.getStringAttribute("id"), sql);
			}
		}
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
	
	private static String parseSql(XNode node,Map<String, String> includeContents) {
	    StringBuilder sql = new StringBuilder();
	    NodeList children = node.getNode().getChildNodes();
	    for (int i = 0; i < children.getLength(); i++) {
	      XNode child = node.newXNode(children.item(i));
	      String data;
	      if("#text".equals(child.getName())){
	    	  data = child.getStringBody("");
	      }else if("include".equals(child.getName())){
	    	  String refId = child.getStringAttribute("refid");
	    	  data = child.toString();
	    	  data = data.replaceAll("<\\s?include.*("+refId+").*>", includeContents.get(refId));
	    	  
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
