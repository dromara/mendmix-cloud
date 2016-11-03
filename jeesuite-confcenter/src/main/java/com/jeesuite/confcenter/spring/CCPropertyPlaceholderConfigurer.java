/**
 * 
 */
package com.jeesuite.confcenter.spring;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月2日
 */
public class CCPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer{

	@Override
	protected Properties mergeProperties() throws IOException {
		Properties properties = super.mergeProperties();
		return properties;
		//
	}

	
}
