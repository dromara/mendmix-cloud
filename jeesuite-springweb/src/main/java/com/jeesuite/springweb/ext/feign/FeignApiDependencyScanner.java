package com.jeesuite.springweb.ext.feign;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;

import com.jeesuite.common.util.ClassScanner;

/**
 * 
 * <br>
 * Class Name   : FeignApiDependencyParser
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年6月15日
 */
public class FeignApiDependencyScanner {

	public static List<String> doScan(List<String> classNames){
		List<String> svcNames = new ArrayList<>();
		
		Class<?> clazz = null;
		Set<String> apiPackages = new HashSet<>();
		for (String className : classNames) {
			try {
				clazz = Class.forName(className);
			} catch (ClassNotFoundException e) {
				continue;
			}
			if(clazz.isAnnotationPresent(EnableFeignClients.class)) {
				String[] basePackages = clazz.getAnnotation(EnableFeignClients.class).basePackages();
				for (String p : basePackages) {
					apiPackages.add(p.substring(0,p.lastIndexOf(".")));
				}
			}
		}
		
		List<String> apiClassNames;
		for (String p : apiPackages) {
			apiClassNames = ClassScanner.scan(p);
			for (String className : apiClassNames) {
				try {
					clazz = Class.forName(className);
				} catch (ClassNotFoundException e) {
					continue;
				}
				if(clazz.isAnnotationPresent(FeignClient.class)) {
					svcNames.add(clazz.getAnnotation(FeignClient.class).value());
				}
			}
		}

		return svcNames;
	}
}
