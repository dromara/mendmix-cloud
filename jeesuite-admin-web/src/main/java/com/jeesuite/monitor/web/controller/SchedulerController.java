/**
 * 
 */
package com.jeesuite.monitor.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.scheduler.model.JobGroupInfo;
import com.jeesuite.scheduler.monitor.MonitorCommond;
import com.jeesuite.scheduler.monitor.SchedulerMonitor;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月30日
 */
public class SchedulerController extends BaseController {
	
	private static final SchedulerMonitor monitor = new SchedulerMonitor(ResourceUtils.get("scheduler.registry", "zookeeper"), ResourceUtils.get("scheduler.registry.servers"));

	public void list(){
		List<JobGroupInfo> groups = monitor.getAllJobGroups();
		setAttr("jobGroups", groups);
		render("list.html");
	}
	
	public void operator(){
		String event = getPara(0);
		String group = getPara("group");
		String job = getPara("job");
		
		MonitorCommond cmd = null;
		if("exec".equals(event)){
			cmd = new MonitorCommond(MonitorCommond.TYPE_EXEC, group, job, null);
		}else if("updatestate".equals(event)){
			cmd = new MonitorCommond(MonitorCommond.TYPE_STATUS_MOD, group, job, null);
		}
		
		Map<String, Object> map = new HashMap<>();
		if(cmd != null){
			try {				
				monitor.publishEvent(cmd);
			    ajaxSuccess("发送执行事件成功");
			} catch (Exception e) {
				map.put("status", 0);
				ajaxError("publish Event发生错误");
			}
		}else{
			map.put("status", 0);
			ajaxError("未知事件");
		}
		renderJson(map);
		
	}
}
