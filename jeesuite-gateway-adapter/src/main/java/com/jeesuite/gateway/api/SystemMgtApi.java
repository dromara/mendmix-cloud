package com.jeesuite.gateway.api;

import java.util.List;

import com.jeesuite.gateway.model.BizSystemModule;

/**
 * 
 * 
 * <br>
 * Class Name : SystemMgrApi
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2020-10-19
 */
public interface SystemMgtApi {

	List<BizSystemModule> getSystemModules();

}
