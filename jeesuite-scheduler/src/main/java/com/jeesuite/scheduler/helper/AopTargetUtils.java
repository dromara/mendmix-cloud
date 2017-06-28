package com.jeesuite.scheduler.helper;

import java.lang.reflect.Field;  

import org.springframework.aop.framework.AdvisedSupport;  
import org.springframework.aop.framework.AopProxy;  
import org.springframework.aop.support.AopUtils;  
  
public class AopTargetUtils {  
  
      
    /** 
     * 获取 目标对象 
     * @param proxy 代理对象 
     * @return  
     * @throws Exception 
     */  
    public static Object getTarget(Object proxy) throws Exception {  
          
        if(!AopUtils.isAopProxy(proxy)) {  
            return proxy;//不是代理对象  
        }  
          
        if(AopUtils.isJdkDynamicProxy(proxy)) {  
            Object object = getJdkDynamicProxyTargetObject(proxy);  
            return getTarget(object);
        } else { //cglib  
        	Object object =  getCglibProxyTargetObject(proxy);  
        	return getTarget(object);
        }  
          
          
          
    }  
  
  
    private static Object getCglibProxyTargetObject(Object proxy) throws Exception {  
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");  
        h.setAccessible(true);  
        Object dynamicAdvisedInterceptor = h.get(proxy);  
          
        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");  
        advised.setAccessible(true);  
          
        Object target = ((AdvisedSupport)advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();  
          
        return target;  
    }  
  
  
    private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {  
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");  
        h.setAccessible(true);  
        AopProxy aopProxy = (AopProxy) h.get(proxy);  
          
        Field advised = aopProxy.getClass().getDeclaredField("advised");  
        advised.setAccessible(true);  
          
        Object target = ((AdvisedSupport)advised.get(aopProxy)).getTargetSource().getTarget();  
          
        return target;  
    }  
      
}  