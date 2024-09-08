/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.scheduler.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.WebUtils;
import org.dromara.mendmix.scheduler.JobContext;
import org.dromara.mendmix.scheduler.model.JobConfig;
import org.dromara.mendmix.scheduler.model.JobGroupInfo;
import org.dromara.mendmix.scheduler.monitor.SchManageCommond;
import org.dromara.mendmix.scheduler.registry.AbstarctJobRegistry;

import com.google.common.io.CharStreams;

/**
 * 
 * 
 * <br>
 * Class Name   : ScheduleApiServlet
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2021-04-05
 */
//@WebServlet(urlPatterns = "/scheduler/*", description = "定时任务API")
public class ScheduleApiServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String act = req.getPathInfo().substring(1);
	
		String respJson = null;
		if(!WebUtils.isInternalRequest(req)) {
			respJson = "{\"code\": 403}";
		}else if("list".equals(act)) {
			JobGroupInfo info = new JobGroupInfo();
			info.setName(JobContext.getContext().getGroupName());
			info.setClusterNodes(new ArrayList<>(JobContext.getContext().getActiveNodes()));

			List<JobConfig> jobs = JobContext.getContext().getRegistry().getAllJobs();
			info.setJobs(jobs);
			respJson = JsonUtils.toJson(info);
		} else if("POST".equals(req.getMethod()) && "command".equals(act)){
			String postJson = CharStreams.toString(new InputStreamReader(req.getInputStream(), "UTF-8"));
			SchManageCommond cmd = JsonUtils.toObject(postJson, SchManageCommond.class);
			AbstarctJobRegistry registry = (AbstarctJobRegistry)JobContext.getContext().getRegistry();
			registry.execCommond(cmd);
			
			respJson = "{\"code\": 200}";
		}
		

		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.setContentType("application/json; charset=utf-8");
		PrintWriter out = null;
		try {
			out = resp.getWriter();
			out.append(respJson);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.close();
			}
		}

	}

}
