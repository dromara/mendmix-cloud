/**
 * 
 */
package com.jeesuite.mybatis.page;

import java.util.List;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月19日
 */
public interface PageDataLoader<T> {

	List<T> load();
}
