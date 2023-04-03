/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.mybatis.parser;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.w3c.dom.NodeList;

import com.mendmix.common.util.ResourceUtils;
import com.mendmix.mybatis.datasource.DataSourceConfig;
import com.mendmix.mybatis.kit.MybatisSqlUtils;
import com.mendmix.mybatis.metadata.MapperMetadata;

/**
 * mybatismapper数据库字段与实体字段映射关系转换工具
 * 
 * @author jwww
 * @date 2015年5月7日上午11:30:42
 * @description <br>
 *              Copyright (c) 2015, vakinge@gmail.com.
 */
public class MybatisMapperParser {

	private static final Logger log = LoggerFactory.getLogger(MybatisMapperParser.class);

	private static Map<String, List<MapperMetadata>> entitiesGroupMap = new HashMap<>();
	private static Map<String, MapperMetadata> mapperKeyMappings = new HashMap<>();

	public static void addMapperLocations(String group, Resource[] mapperLocations) {
		doParse(group, mapperLocations);
	}

	public static List<MapperMetadata> getMapperMetadatas(String group) {
		return entitiesGroupMap.get(group);
	}

	public static MapperMetadata getMapperMetadata(String mapperOrEntityName) {
		return mapperKeyMappings.get(mapperOrEntityName);
	}

	private synchronized static void doParse(String group, Resource[] mapperLocations) {
		if (entitiesGroupMap.containsKey(group))
			return;

		Map<String, MapperMetadata> entityInfos = new HashMap<>();
		try {
			//
			String propKeyPrefix = "";
			if (!DataSourceConfig.DEFAULT_GROUP_NAME.equals(group)) {
				propKeyPrefix = "group[" + group + "].";
			}
			List<String> mapperPackages = ResourceUtils.getList(propKeyPrefix + "mybatis.mapper-package");
			for (String mapperPackage : mapperPackages) {
				String classNameToResourcePath = ClassUtils.convertClassNameToResourcePath(mapperPackage);
				String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + classNameToResourcePath
						+ "/**/*.class";
				PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
				Resource[] resources = resolver.getResources(pattern);
				for (Resource res : resources) {
					String mapperClass = StringUtils.splitByWholeSeparator(res.getURI().getPath(),
							classNameToResourcePath)[1];
					mapperClass = mapperPackage + "."
							+ mapperClass.substring(1).substring(0, mapperClass.lastIndexOf(".") - 1);
					mapperClass  = ClassUtils.convertResourcePathToClassName(mapperClass);
					entityInfos.put(mapperClass, new MapperMetadata(mapperClass));
				}
			}
		} catch (Exception e) {
		}
		// 解析xml
		try {
			for (Resource resource : mapperLocations) {
				parseMapperFile(entityInfos, resource.getFilename(), resource.getInputStream());
			}
		} catch (Exception e) {
			log.error("解析mapper文件异常", e);
			throw new RuntimeException("解析mapper文件异常");
		}
		
		List<MapperMetadata> list = entityInfos.values().stream().filter(e -> e.getEntityClass() != null).collect(Collectors.toList());
		for (MapperMetadata mapperMetadata : list) {
			mapperMetadata.setGroup(group);
		}
		entitiesGroupMap.put(group, list);
		log.info("MENDMIX-TRACE-LOGGGING-->> parse group[{}] finish,size:{}", group, entityInfos.size());

	}

	private static void parseMapperFile(Map<String, MapperMetadata> entityInfos, String fileName, InputStream inputStream)
			throws Exception {
		XPathParser parser = new XPathParser(inputStream,true, null, new XMLMapperEntityResolver());
		XNode evalNode = parser.evalNode("/mapper");
		
		String mapperClass = evalNode.getStringAttribute("namespace");
		
		MapperMetadata entityInfo = entityInfos.get(mapperClass);
		if(entityInfo == null) {
			entityInfo = new MapperMetadata(mapperClass);
			entityInfos.put(mapperClass, entityInfo);
		}
		if(entityInfo.getEntityClass() == null){				
			log.warn("MENDMIX-TRACE-LOGGGING-->> can't parse entityClass for:{}",mapperClass);
			return;
		}
		mapperKeyMappings.put(mapperClass, entityInfo);
		mapperKeyMappings.put(entityInfo.getEntityClass().getName(), entityInfo);
		Map<String, String> includes = new HashMap<>();
		List<XNode> children = evalNode.getChildren();
		for (XNode xNode : children) {
			if("sql".equalsIgnoreCase(xNode.getName())){
				includes.put(xNode.getStringAttribute("id"), xNode.getStringBody());
				continue;
			}
		}
		for (XNode xNode : children) {
			if ("select|insert|update|delete".contains(xNode.getName().toLowerCase())) {
				StringBuilder sql = new StringBuilder();
				parseSql(sql,xNode,includes);
				entityInfo.addSql(xNode.getName().toLowerCase(),xNode.getStringAttribute("id"), sql.toString());
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

	public static void main(String[] args) throws Exception {
		String path = "/Users/jiangwei/project/jeesuite-libs/mendmix-mybatis/src/test/resources/mapper/template.xml";
		InputStream inputStream = new FileInputStream(new File(path));
		XPathParser parser = new XPathParser(inputStream,true, null, new XMLMapperEntityResolver());
		XNode evalNode = parser.evalNode("/mapper");
		List<XNode> children = evalNode.getChildren();
		Map<String, String> includes = new HashMap<>();
		for (XNode xNode : children) {
			if("sql".equalsIgnoreCase(xNode.getName())){
				includes.put(xNode.getStringAttribute("id"), xNode.getStringBody());
			}
		}
		for (XNode xNode : children) {
			if ("select|insert|update|delete".contains(xNode.getName().toLowerCase())) {
				StringBuilder sql = new StringBuilder();
				parseSql(sql,xNode,includes);
				System.out.println(">>>>>parseSqlUseTables:" + MybatisSqlUtils.parseSqlUseTables(sql.toString()));
			}
		}
		
		inputStream.close();
	}
}
