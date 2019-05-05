package com.jeesuite.mybatis.plugin.pagination;

import java.util.ArrayList;
import java.util.List;

public class PageExecutor {

	private static ThreadLocal<PageParams> pageParamsHolder = new ThreadLocal<>();
	
	public static interface PageDataLoader<T> {
		List<T> load();
	}
	
	public static interface ConvertPageDataLoader<E,V> extends PageDataLoader<E>{
		V convert(E e);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Page<T> pagination(PageParams pageParams,PageDataLoader<T> dataLoader ){
		pageParamsHolder.set(pageParams);
		List<T> list = dataLoader.load();
		return (Page<T>) list.get(0);
	}
	
	@SuppressWarnings("unchecked")
	public static <E,V> Page<V> pagination(PageParams pageParams,ConvertPageDataLoader<E,V> dataLoader ){
		pageParamsHolder.set(pageParams);
		List<E> list = dataLoader.load();
		Page<E> page = (Page<E>) list.get(0);
		List<V> convertDatas = new ArrayList<>(page.getData().size());
		for (E e : page.getData()) {
			convertDatas.add(dataLoader.convert(e));
		}
		return new Page<>(pageParams, page.getTotal(), convertDatas);
	}
	
	public static PageParams getPageParams(){
		return pageParamsHolder.get();
	}
	
	public static void clearPageParams(){
		pageParamsHolder.remove();
	}
	
}
