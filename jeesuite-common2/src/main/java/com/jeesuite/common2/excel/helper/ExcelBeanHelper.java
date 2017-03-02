package com.jeesuite.common2.excel.helper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common2.excel.ExcelOperBaseException;
import com.jeesuite.common2.excel.annotation.TitleCell;
import com.jeesuite.common2.excel.model.ExcelMeta;
import com.jeesuite.common2.excel.model.TitleMeta;

public class ExcelBeanHelper {

	private static Map<String, Map<String, PropertyDescriptor>> aliasPropertyDescriptorCache = new ConcurrentHashMap<String, Map<String, PropertyDescriptor>>();

	private static Map<String, ExcelMeta> titleCellBeanCache = new ConcurrentHashMap<String, ExcelMeta>();

	public static <T> List<T> setRowValues(Class<T> clazz,List<String> contents){

		try {	
			Map<String, PropertyDescriptor> pds = getAliasPropertyDescriptors(clazz);
			
			List<T> results = new ArrayList<>();
			if(contents.isEmpty())return results;
			
			String[] titles = getExcelMeta(clazz).getFinalTitles();
			
			int titleRowCount = getExcelMeta(clazz).getTitleRowNum();
			
//			//解析内容标题内容为数组
//			String[][] contentTitles = new String[titleRowCount][];
//			for (int i = 0; i < titleRowCount; i++) {
//				contentTitles[i] = contents.get(i).split(ExcelValidator.FIELD_SPLIT);
//			}
//			
//			//校验excel的title和定义bean的title是否一致
//			boolean colMatched;
//            for (int i = 0; i < titles.length; i++) {
//            	colMatched = false;
//				inner:for (int j = 0; j < contentTitles.length; j++) {
//					String contentTitle = clearWrapper(contentTitles[j][i]);
//					System.out.println(titles[i] + "--" + contentTitle);
//					if(colMatched = titles[i].equals(contentTitle)){
//						break inner;
//					}
//				}
//            	if(!colMatched)throw new ExcelOperBaseException("没有找到列["+titles[i] + "]");
//			}
			
			String[] vals = null;
			for (int i = titleRowCount; i < contents.size(); i++) {
				T instance = clazz.newInstance();
				vals = contents.get(i).split(ExcelValidator.FIELD_SPLIT);

				boolean anyColumnNotEmpty = false;
				inner:for (int j = 0; j < titles.length; j++) {
					if(vals.length < j + 1 )break inner;
					anyColumnNotEmpty = anyColumnNotEmpty || StringUtils.isNotBlank(vals[j]);
					PropertyDescriptor propertyDescriptor = pds.get(clearWrapper(titles[j]).trim());
					if(propertyDescriptor != null && vals[j] != null){
						try {
							Object rawValue = rawValue(vals[j],propertyDescriptor.getPropertyType());
							propertyDescriptor.getWriteMethod().invoke(instance, rawValue);
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
				}
				if(anyColumnNotEmpty){					
					results.add(instance);
				}
			}
			
			return results;
		} catch (Exception e) {
			if(e instanceof ExcelOperBaseException)throw (ExcelOperBaseException)e;
			throw new BeanConverterException(e);
		}
	
	}
	
	public static <T> List<Object[]> beanToExcelValueArrays(List<T> datas,Class<T> clazz){
		
		Map<String, PropertyDescriptor> pds = getAliasPropertyDescriptors(clazz);
		
		List<Object[]> result = new ArrayList<>(datas.size() + 1);
		
		String[] titles = getExcelMeta(clazz).getFinalTitles();
		
		if(datas == null || datas.isEmpty())return result;
			
		int valNums = titles.length;
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
	
	
	public static ExcelMeta getExcelMeta(Class<?> clazz){
		if(titleCellBeanCache.isEmpty())getAliasPropertyDescriptors(clazz);
		return titleCellBeanCache.get(clazz.getCanonicalName());
	}

	
	 private static Object rawValue(String value,Class<?> propertyType){
		    value = clearWrapper(value);
	    	Object result = value;
	    	if (propertyType == String.class) {
	    		return result;
	        }else if (propertyType == BigDecimal.class) {
	    		result = new BigDecimal(value.replaceAll(",", ""));
	        } else if (propertyType == byte.class || propertyType == Byte.class) {
	        	result = Byte.valueOf(value);
	        } else if (propertyType == short.class || propertyType == Short.class) {
	        	result = Short.valueOf(value.replaceAll(",", ""));
	        } else if (propertyType == int.class || propertyType == Integer.class) {
	        	result = Integer.parseInt(value.replaceAll(",", ""));
	        } else if (propertyType == double.class || propertyType == Double.class) {
	        	result = Double.valueOf(value.replaceAll(",", ""));
	        }else if (propertyType == float.class || propertyType == Float.class) {
	        	result = Float.parseFloat(value.replaceAll(",", ""));
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

		List<TitleMeta> titleMetas = new ArrayList<>();

		BeanInfo srcBeanInfo = Introspector.getBeanInfo(clazz);

		PropertyDescriptor[] descriptors = srcBeanInfo.getPropertyDescriptors();
		
		Map<String, TitleMeta> parentMap = new HashMap<>();
		int maxRow = 1;
		
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
					
					TitleMeta cell = new TitleMeta(annotation.name());
					cell.setValueType(annotation.type());
					
					if(StringUtils.isBlank(annotation.parentName())){
						cell.setColumnIndex(annotation.column());
						cell.setRowIndex(annotation.row());
						titleMetas.add(cell);
					}else{
						TitleMeta cellParent = parentMap.get(annotation.parentName());
						if(cellParent == null){
							cellParent = new TitleMeta(annotation.parentName());
							cellParent.setValueType(annotation.type());
							cellParent.setColumnIndex(annotation.column());
							parentMap.put(annotation.parentName(), cellParent);
							titleMetas.add(cellParent);
						}
						cell.setColumnIndex(annotation.column());
						cell.setRowIndex(annotation.row());
						cellParent.addChildren(cell);
						
						maxRow = annotation.row() > maxRow ? annotation.row() : maxRow;
					}
				}
			}
			
		}
		
		
        
        ExcelMeta excelMeta = new ExcelMeta(clazz,titleMetas,maxRow);
        titleCellBeanCache.put(canonicalName, excelMeta);
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
