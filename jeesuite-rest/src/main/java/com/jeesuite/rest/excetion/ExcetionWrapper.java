/**
 * 
 */
package com.jeesuite.rest.excetion;

import com.jeesuite.rest.response.RestResponse;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月23日
 */
public interface ExcetionWrapper {

	RestResponse toResponse(Exception e);
}
