/**
 * 
 */
package com.jeesuite.mybatis.page;

import java.util.List;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月19日
 */
public class PageTemplate {

	public static <T> PageInfo<T> execute(int pageNo,int pageSize,PageDataLoader<T> dataLoader){
		PageHelper.startPage(pageNo, pageSize);
		List<T> list = dataLoader.load();
		return new PageInfo<T>(list);
	}
}
