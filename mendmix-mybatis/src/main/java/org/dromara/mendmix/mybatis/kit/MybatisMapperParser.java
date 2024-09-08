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
package org.dromara.mendmix.mybatis.kit;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.mybatis.datasource.DataSourceConfig;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;
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
	
	private static Map<String, List<MapperMetadata>> groupMapperMetadataMap = new HashMap<>();
	private static Map<String,MapperMetadata> mapperMetadataMappings = new HashMap<>();
	
	public static void addMapperLocations(String group,Resource[] mapperLocations){
		doParse(group, mapperLocations);
	}
	
	public static List<MapperMetadata> getMapperMetadatas(String group) {
		return groupMapperMetadataMap.get(group);
	}
	
	public static MapperMetadata getMapperMetadata(String mapperOrEntityName){
		return mapperMetadataMappings.get(mapperOrEntityName);
	}
	

	private synchronized static void doParse(String group,Resource[] mapperLocations){
		if(groupMapperMetadataMap.containsKey(group))return;
		
		Map<String,MapperMetadata> entityInfos = new HashMap<>();
		String mapperPath = null;
		try {
			//
			String propKeyPrefix = "";
			if(!DataSourceConfig.DEFAULT_GROUP_NAME.equals(group)) {
				propKeyPrefix = "group["+group+"].";
			}
			List<String> mapperPackages = ResourceUtils.getList(propKeyPrefix + "mybatis.mapper-package");
			for (String mapperPackage : mapperPackages) {
				String classNameToResourcePath = ClassUtils.convertClassNameToResourcePath(mapperPackage);
				String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + classNameToResourcePath + "/**/*.class";
				PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
				Resource[] resources = resolver.getResources(pattern);
				for(Resource res :resources) {
					mapperPath = StringUtils.defaultString(res.getURI().getPath(), res.getURI().toString());
					String mapperClass;
					if(mapperPath.contains(classNameToResourcePath)) {
						mapperClass = StringUtils.splitByWholeSeparator(mapperPath, classNameToResourcePath)[1];
						String className = mapperClass.substring(1).substring(0,mapperClass.lastIndexOf(".") - 1);
						mapperClass = ClassUtils.convertResourcePathToClassName(mapperPackage + "." + className);
					}else {
						String packagePath = StringUtils.splitByWholeSeparator(classNameToResourcePath, "*")[0];
						if(mapperPath.contains(".jar")) {
							mapperPath = StringUtils.splitByWholeSeparator(mapperPath, ".jar")[1];
						}
						packagePath = packagePath + StringUtils.splitByWholeSeparator(mapperPath, packagePath)[1];
						packagePath = packagePath.substring(0,packagePath.lastIndexOf("."));
						mapperClass = ClassUtils.convertResourcePathToClassName(packagePath);
					}
					entityInfos.put(mapperClass, new MapperMetadata(mapperClass));
				}
			}
		} catch (Exception e) {
			System.err.println("parseMapperXML["+mapperPath+"] error:" + e.getMessage()) ;
		}

		//解析xml
		try {
			for (Resource resource : mapperLocations) {
				parseMapperFile(entityInfos,resource.getFilename(),resource.getInputStream());
			}
		} catch (Exception e) {
			log.error("解析mapper文件异常", e);	
			throw new RuntimeException("解析mapper文件异常");
		}
		//
		List<MapperMetadata> entitys = entityInfos.values().stream().filter(e -> e.getEntityClass() != null).collect(Collectors.toList());
		groupMapperMetadataMap.put(group, entitys);
		log.info("<startup-logging>  parse group[{}] finish,size:{}",group,entityInfos.size());
	}
	
	
	private static void parseMapperFile(Map<String,MapperMetadata> entityInfos,String fileName,InputStream inputStream) throws Exception {
		
		
		XPathParser parser = new XPathParser(inputStream,true, null, new XMLMapperEntityResolver());
		XNode evalNode = parser.evalNode("/mapper");
		
		String mapperClass = evalNode.getStringAttribute("namespace");
		
		MapperMetadata entityInfo = entityInfos.get(mapperClass);
		if(entityInfo == null) {
			entityInfo = new MapperMetadata(mapperClass);
			entityInfos.put(mapperClass, entityInfo);
		}
		
		if(entityInfo.getEntityClass() == null){				
			log.warn("can't parse entityClass for:{}",mapperClass);
			return;
		}
		mapperMetadataMappings.put(mapperClass, entityInfo);
		
		Map<String, String> includes = new HashMap<>();
		List<XNode> children = evalNode.getChildren();
		for (XNode xNode : children) {
			if("sql".equalsIgnoreCase(xNode.getName())){
				includes.put(xNode.getStringAttribute("id"), xNode.getStringBody());
				continue;
			}
		}

		for (XNode xNode : children) {
			if ("select".contains(xNode.getName().toLowerCase())) {
				StringBuilder sql = new StringBuilder();
				parseSql(sql,xNode,includes);
				entityInfo.parseSqlUseTables(xNode.getStringAttribute("id"), sql.toString());
			}
		}
		
		inputStream.close();
	}
	
	private static void parseSql(StringBuilder result,XNode node, Map<String, String> includeContents) {
		NodeList children = node.getNode().getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			XNode child = node.newXNode(children.item(i));
			String data = null;
			NodeList subChildren = child.getNode().getChildNodes();
			if (subChildren.getLength() > 1) {
				result.append("<").append(child.getName()).append(">");
				parseSql(result, child, includeContents);
				result.append("</").append(child.getName()).append(">");
			}else {
				if ("#text".equals(child.getName())) {
					data = child.getStringBody("");
				} else if ("include".equals(child.getName())) {
					String refId = child.getStringAttribute("refid");
					data = child.toString();
					if (includeContents.containsKey(refId)) {
						data = data.replaceAll("<\\s?include.*(" + refId + ").*?(?=>)>", includeContents.get(refId));
					}
				}else if("bind".equals(child.getName())) {
					data = "";
				} else {
					data = child.toString();
				}
				data = data.replaceAll("\n{2,}", "\n");
				result.append(data);
			}
		}
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
}
