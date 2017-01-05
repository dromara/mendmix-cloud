package com.jeesuite.common2.excel.helper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common2.excel.annotation.TitleCell;
import com.jeesuite.common2.excel.model.TitleCellBean;

public class ExcelBeanHelper {

	private static Map<String, Map<String, PropertyDescriptor>> aliasPropertyDescriptorCache = new ConcurrentHashMap<String, Map<String, PropertyDescriptor>>();

	private static Map<String, List<TitleCellBean>> titleCellBeanCache = new ConcurrentHashMap<String, List<TitleCellBean>>();

	private static Map<String, String[]> titlesCache = new ConcurrentHashMap<String, String[]>();
	
	private static Map<String, Integer> titleRowCache = new ConcurrentHashMap<String, Integer>();

	
	public static <T> List<T> setRowValues(Class<T> clazz,List<String> contents){

		try {	
			Map<String, PropertyDescriptor> pds = getAliasPropertyDescriptors(clazz);
			
			List<T> results = new ArrayList<>();
			if(contents.isEmpty())return results;
			String[] titles = titlesCache.get(clazz.getCanonicalName());
			int titleRowCount = titleRowCache.get(clazz.getCanonicalName());
			String[] vals = null;
			for (int i = titleRowCount; i < contents.size(); i++) {
				T instance = clazz.newInstance();
				vals = contents.get(i).split(ExcelValidator.FIELD_SPLIT);

				for (int j = 0; j < vals.length; j++) {
					PropertyDescriptor propertyDescriptor = pds.get(clearWrapper(titles[j]).trim());
					if(propertyDescriptor != null && vals[j] != null){
						Object rawValue = rawValue(vals[j],propertyDescriptor.getPropertyType());
						propertyDescriptor.getWriteMethod().invoke(instance, rawValue);
					}
				}
				results.add(instance);
			}
			
			return results;
		} catch (Exception e) {
			throw new BeanConverterException(e);
		}
	
	}
	
	public static <T> List<Object[]> beanToExcelValueArrays(List<T> datas,Class<T> clazz){
		
		Map<String, PropertyDescriptor> pds = getAliasPropertyDescriptors(clazz);
		
		List<Object[]> result = new ArrayList<>(datas.size() + 1);
		
		String[] titles = titlesCache.get(clazz.getCanonicalName());
		
		result.add(titles);
		
		if(datas == null || datas.isEmpty())return result;
		
		
		int valNums = titlesCache.get(clazz.getCanonicalName()).length;
		Object[] valArrs;
		for (T e : datas) {
			valArrs = new Object[valNums];
			try {				
				for (int i = 0; i < titles.length; i++) {
					PropertyDescriptor descriptor = pds.get(titles[i]);
					if(descriptor != null){
						valArrs[i] = descriptor.getReadMethod().invoke(e);
					}
				}
				result.add(valArrs);
			} catch (Exception e2) {
				throw new BeanConverterException(e2);
			}
		}
		return result;
	}

	
	 private static Object rawValue(String value,Class<?> propertyType){
		    value = clearWrapper(value);
	    	Object result = value;
	    	if (propertyType == String.class) {
	    		return result;
	        }else if (propertyType == BigDecimal.class) {
	    		result = new BigDecimal(value);
	        } else if (propertyType == byte.class || propertyType == Byte.class) {
	        	result = Byte.valueOf(value);
	        } else if (propertyType == short.class || propertyType == Short.class) {
	        	result = Short.valueOf(value.toString());
	        } else if (propertyType == int.class || propertyType == Integer.class) {
	        	result = Integer.parseInt(value);
	        } else if (propertyType == double.class || propertyType == Double.class) {
	        	result = Double.valueOf(value.toString());
	        } else if (propertyType == Date.class) {
	        	if(value != null){
	        		//TODO
	        	}
	            
	        } else if (propertyType == boolean.class || propertyType == Boolean.class) {
	        	result = Boolean.parseBoolean(value);
	        } 
	    	return result;
	    }
	
	private static Map<String, PropertyDescriptor> getAliasPropertyDescriptors(Class<?> clazz) {
		try {
			String canonicalName = clazz.getCanonicalName();
			Map<String, PropertyDescriptor> map = aliasPropertyDescriptorCache.get(canonicalName);
			
			if (map == null) {
				doCacheClass(clazz, canonicalName);
				map = aliasPropertyDescriptorCache.get(canonicalName);
			}
			
			return map;
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param clazz
	 * @param canonicalName
	 * @return
	 * @throws IntrospectionException
	 */
	private synchronized static void doCacheClass(Class<?> clazz, String canonicalName)
			throws Exception {
		if (aliasPropertyDescriptorCache.containsKey(canonicalName))
			return;

		Map<String, PropertyDescriptor> map = new HashMap<>();
		Map<String, PropertyDescriptor> aliasMap = new HashMap<>();

		List<TitleCellBean> titleCellBeans = new ArrayList<>();

		BeanInfo srcBeanInfo = Introspector.getBeanInfo(clazz);

		PropertyDescriptor[] descriptors = srcBeanInfo.getPropertyDescriptors();
		
		Map<String, TitleCellBean> parentMap = new HashMap<>();
		int index = 0,subIndex = 0,titleRow = 1;
		
		for (PropertyDescriptor descriptor : descriptors) {

			String name = descriptor.getName();
			
			if("class".equals(name))continue;

			Method readMethod = descriptor.getReadMethod();
			Method writeMethod = descriptor.getWriteMethod();

			
			if (readMethod == null)
				try {
					readMethod = clazz.getMethod("get" + name.substring(0, 1).toUpperCase() + name.substring(1));

					descriptor.setReadMethod(readMethod);
				} catch (NoSuchMethodException | SecurityException e) {
				}

			if (writeMethod == null)
				try {
					writeMethod = clazz.getMethod("set" + name.substring(0, 1).toUpperCase() + name.substring(1),
							descriptor.getPropertyType());

					descriptor.setWriteMethod(writeMethod);
				} catch (NoSuchMethodException | SecurityException e) {
				}

			if (readMethod != null || writeMethod != null) {
				map.put(descriptor.getName(), descriptor);
				//
				TitleCell annotation = null;
				try {
					annotation = clazz.getDeclaredField(name).getAnnotation(TitleCell.class);
				} catch (NoSuchFieldException e) {
					annotation = clazz.getSuperclass().getDeclaredField(name).getAnnotation(TitleCell.class);
				}
				if(annotation != null){
					aliasMap.put(annotation.name().trim(), descriptor);
					
					TitleCellBean cell = new TitleCellBean(annotation.name());
					
					if(StringUtils.isBlank(annotation.parentName())){
						cell.setColumnIndex(++index);
						titleCellBeans.add(cell);
					}else{
						TitleCellBean cellParent = parentMap.get(annotation.parentName());
						if(cellParent == null){
							subIndex = index;
							cellParent = new TitleCellBean(annotation.parentName());
							cellParent.setColumnIndex(++index);
							parentMap.put(annotation.parentName(), cellParent);
							titleCellBeans.add(cellParent);
						}
						cell.setColumnIndex(++subIndex);
						cell.setRowIndex(1);
						cellParent.addChildren(cell);
						
						titleRow = 2;
					}
				}
			}
			
		}
		
		//排序
		Collections.sort(titleCellBeans, new Comparator<TitleCellBean>() {
			@Override
			public int compare(TitleCellBean o1, TitleCellBean o2) {
				return o1.getColumnIndex() - o2.getColumnIndex();
			}
		});
		
		List<String> titleList = new ArrayList<>();
        for (int i = 0; i < titleCellBeans.size(); i++) {
        	if(titleCellBeans.get(i).getChildren().isEmpty()){
        		titleList.add(titleCellBeans.get(i).getTitle());
        	}else{
        		List<TitleCellBean> children = titleCellBeans.get(i).getChildren();
        		for (TitleCellBean titleCell : children) {
        			titleList.add(titleCell.getTitle());
				}
        	}
		}
        
        titleRowCache.put(canonicalName, titleRow);
        titleCellBeanCache.put(canonicalName, titleCellBeans);
        titlesCache.put(canonicalName, titleList.toArray(new String[0]));
		aliasPropertyDescriptorCache.put(canonicalName, aliasMap);
	}
	
	private static String clearWrapper(String orig){
		return orig.replaceAll(ExcelValidator.QUOTE, "");
	}
	
	public static class BeanConverterException extends RuntimeException {
        private static final long serialVersionUID = 152873897614690397L;

        public BeanConverterException(Throwable cause) {
            super(cause);
        }
    }
	

}
