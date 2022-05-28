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
package com.mendmix.mybatis.plugin.pagination;

import java.util.ArrayList;
import java.util.List;

import com.mendmix.common.model.Page;
import com.mendmix.common.model.PageParams;

public class PageExecutor {

	private static ThreadLocal<PageParams> pageParamsHolder = new ThreadLocal<>();
	
	public static interface PageDataLoader<T> {
		List<T> load();
	}
	
	public static interface ConvertPageDataLoader<E,V> extends PageDataLoader<E>{
		V convert(E e);
	}
	
	@SuppressWarnings("unchecked")
	private static <T> Page<T> doPagination(PageParams pageParams,PageDataLoader<T> dataLoader ){
		try {			
			pageParamsHolder.set(pageParams);
			List<T> list = dataLoader.load();
			return (Page<T>) list.get(0);
		} finally {
			pageParamsHolder.remove();
		}
	}
	
	public static <T> Page<T> pagination(PageParams pageParams,PageDataLoader<T> dataLoader ){
		return doPagination(pageParams, dataLoader);
	}
	
	public static <E,V> Page<V> pagination(PageParams pageParams,ConvertPageDataLoader<E,V> dataLoader ){
		
		Page<E> page = doPagination(pageParams, dataLoader);
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
