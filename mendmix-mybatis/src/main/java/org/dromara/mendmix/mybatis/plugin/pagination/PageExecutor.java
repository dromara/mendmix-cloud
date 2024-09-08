/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.mybatis.plugin.pagination;

import java.util.ArrayList;
import java.util.List;

import org.dromara.mendmix.common.model.Page;
import org.dromara.mendmix.common.model.PageParams;

public class PageExecutor {

	private static ThreadLocal<Page<?>> pageObjectHolder = new ThreadLocal<>();
	
	public static interface PageDataLoader<T> {
		List<T> load();
		default void afterLoad(List<T> datas) {}
	}
	
	public static interface ConvertPageDataLoader<E,V> extends PageDataLoader<E>{
		V convert(E e);
	}
	
	private static <T> Page<T> doPagination(PageParams pageParam,long total,PageDataLoader<T> dataLoader ){
		Page<T> page = new Page<>(pageParam, total, null);
		try {
			pageObjectHolder.set(page);
			dataLoader.load();
		} finally {
			pageObjectHolder.remove();
		}
		if(page.getData() != null) {
			dataLoader.afterLoad(page.getData());
		}
		return page;
	}
	
	public static <T> Page<T> pagination(PageParams pageParam,PageDataLoader<T> dataLoader ){
		return pagination(pageParam, 0, dataLoader);
	}
	
	public static <T> Page<T> pagination(PageParams pageParam,long total,PageDataLoader<T> dataLoader ){
		return doPagination(pageParam,total, dataLoader);
	}
	
	public static <E,V> Page<V> pagination(PageParams pageParam,ConvertPageDataLoader<E,V> dataLoader ){
		return pagination(pageParam, 0, dataLoader);
	}
	
	public static <E,V> Page<V> pagination(PageParams pageParam,long total,ConvertPageDataLoader<E,V> dataLoader ){
		
		Page<E> page = doPagination(pageParam,total, dataLoader);
		List<V> convertDatas = new ArrayList<>(page.getData().size());
		for (E e : page.getData()) {
			convertDatas.add(dataLoader.convert(e));
		}
		return new Page<>(page, page.getTotal(), convertDatas);
	}
	
	public static void setPageObject(Page<?> page){
		pageObjectHolder.set(page);
	}
	
	public static Page<?> getPageObject(){
		return pageObjectHolder.get();
	}
	
	public static void clearPageObject(){
		pageObjectHolder.remove();
	}
	
}
