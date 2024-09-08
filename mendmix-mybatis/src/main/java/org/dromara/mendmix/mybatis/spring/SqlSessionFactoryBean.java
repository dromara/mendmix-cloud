/*
 * Copyright 2016-2018 dromara.org.
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
package org.dromara.mendmix.mybatis.spring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.Configuration;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultElement;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.mybatis.datasource.DataSourceConfig;
import org.dromara.mendmix.mybatis.kit.MybatisMapperParser;
import org.dromara.mendmix.spring.InstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月22日
 */
public class SqlSessionFactoryBean extends org.mybatis.spring.SqlSessionFactoryBean implements ApplicationContextAware{

	private static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");

	private String groupName = "default";
	private Resource[] mapperLocations;
	
	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	@Override
	public void setMapperLocations(Resource... mapperLocations) {
		Resource[] adapterMapperLocations = getAdapterMapperLocations();
		if(adapterMapperLocations != null && adapterMapperLocations.length > 0) {
			Map<String, Resource> mapperResourceMap = new HashMap<>(mapperLocations.length);
			String fileName;
			for (Resource resource : mapperLocations) {
				fileName = resource.getFilename();
				mapperResourceMap.put(fileName, resource);
			}
			//
			Resource originResource;
            for (Resource resource : adapterMapperLocations) {
            	fileName = resource.getFilename();
            	originResource = mapperResourceMap.get(fileName);
            	if(originResource == null)continue;
            	Resource mergedResource = mergeMapperResource(originResource, resource);
            	mapperResourceMap.put(fileName, mergedResource);
            	logger.info(">>load adapter mapper:{},location:{}",fileName,resource);
			}
            mapperLocations = mapperResourceMap.values().toArray(new Resource[0]);
		}
		this.mapperLocations = mapperLocations;
		super.setMapperLocations(mapperLocations);
	}
	
	private Resource[] getAdapterMapperLocations() {
		String mapperAdapter;
		String mapperLocationsValue;
		if(DataSourceConfig.DEFAULT_GROUP_NAME.equals(groupName)) {
			mapperAdapter = ResourceUtils.getProperty("mybatis.mapper-adapter");
			mapperLocationsValue = ResourceUtils.getProperty("mybatis.mapper-locations");
		}else {
			mapperAdapter = ResourceUtils.getAnyProperty(groupName + ".mybatis.mapper-adapter","group["+groupName+"].mybatis.mapper-adapter");
			mapperLocationsValue = ResourceUtils.getAnyProperty(groupName + ".mybatis.mapper-locations","group["+groupName+"].mybatis.mapper-locations");
		}
		if(StringUtils.isBlank(mapperAdapter)) {
			return null;
		}
		logger.info(">>mybatis mapperAdapter::{}",mapperAdapter);
		//classpath:mapper/*Mapper.xml,mapper2/*Mapper.xml
		String[] mapperLocations = StringUtils.split(mapperLocationsValue, ",");
		
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] allResources = null;
		for (String mapperLocation : mapperLocations) {
			String[] parts = StringUtils.split(mapperLocation, ":", 2);
			String path = parts[1];
			if(path.startsWith("/")) {
				path = path.substring(1);
			}
			String adapterMapperLocations = parts[0] + ":" + mapperAdapter + "-" + path;
			try {
				final Resource[] resources = resolver.getResources(adapterMapperLocations);
                if(allResources == null) {
                	allResources = resources;
                }else {
                	allResources = ArrayUtils.addAll(allResources, resources);
                }
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return allResources;
	}
	
	private Resource mergeMapperResource(Resource originResource,Resource newResource) {
		try {
			SAXReader saxReader = new SAXReader();
			Document originDocument = saxReader.read(originResource.getInputStream());
			Element root = originDocument.getRootElement();
			Iterator<Element> iterator = root.elementIterator();
			Map<String, Element> map = new HashMap<>();
			String key;
			Element child;
			while(iterator.hasNext()) {
				child = iterator.next();
				key = child.getName() + "-" + child.attributeValue("id");
				map.put(key, child);
			}
			Document newDocument = saxReader.read(newResource.getInputStream());
			root = newDocument.getRootElement();
			iterator = root.elementIterator();
			while(iterator.hasNext()) {
				child = iterator.next();
				key = child.getName() + "-" + child.attributeValue("id");
				if(!map.containsKey(key)) {
					continue;
				}
				Element originElement = map.get(key);
				originElement.setContent(((DefaultElement)child).content());
			}
			
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        XMLWriter writer = new XMLWriter(outputStream, OutputFormat.createPrettyPrint());
	        writer.write(originDocument);
	        writer.close();
	        return new ByteArrayResource(outputStream.toByteArray());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		MybatisMapperParser.addMapperLocations(groupName, mapperLocations);
		Configuration configuration = getObject().getConfiguration();
		//
		MybatisEnhanceHelper.handle(groupName,configuration);
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		InstanceFactory.setApplicationContext(applicationContext);
	}
	
	
}
