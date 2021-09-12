/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.mybatis.datasource;

import java.util.List;

/**
 * 
 * <br>
 * Class Name   : DataSourceConfigLoader
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年6月30日
 */
public interface DataSourceConfigLoader {
	
	List<DataSourceConfig> load(String group);
}
