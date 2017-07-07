package com.jeesuite.mybatis.plugin.pagination;

import java.util.List;

public class PageExecutor {

	private static ThreadLocal<PageParams> pageParamsHolder = new ThreadLocal<>();
	
	public static interface PageDataLoader<T> {
		List<T> load();
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Page<T> pagination(PageParams pageParams,PageDataLoader<T> dataLoader ){
		pageParamsHolder.set(pageParams);
		List<T> list = dataLoader.load();
		return (Page<T>) list.get(0);
	}
	
	public static PageParams getPageParams(){
		return pageParamsHolder.get();
	}
	
	public static void clearPageParams(){
		pageParamsHolder.remove();
	}
	
}
