/**
 * 
 */
package com.jeesuite.monitor.web.controller;

import java.util.List;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.scheduler.model.JobGroupInfo;
import com.jeesuite.scheduler.monitor.SchedulerMonitor;
import com.jfinal.core.Controller;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月30日
 */
public class SchedulerController extends Controller {
	
	private static final SchedulerMonitor monitor = new SchedulerMonitor(ResourceUtils.get("scheduler.registry", "zookeeper"), ResourceUtils.get("scheduler.registry.servers"));

	public void list(){
		List<JobGroupInfo> groups = monitor.getAllJobGroups();
		setAttr("jobGroups", groups);
		render("list.html");
	}
}
