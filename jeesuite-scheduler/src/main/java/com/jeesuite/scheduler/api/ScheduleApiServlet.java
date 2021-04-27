package com.jeesuite.scheduler.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.io.CharStreams;
import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.scheduler.JobContext;
import com.jeesuite.scheduler.model.JobConfig;
import com.jeesuite.scheduler.model.JobGroupInfo;
import com.jeesuite.scheduler.monitor.MonitorCommond;
import com.jeesuite.scheduler.registry.AbstarctJobRegistry;

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
@WebServlet(urlPatterns = "/scheduler/*", description = "定时任务API")
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
		if("status".equals(act)) {
			JobGroupInfo info = new JobGroupInfo();
			info.setName(JobContext.getContext().getGroupName());
			info.setClusterNodes(new ArrayList<>(JobContext.getContext().getActiveNodes()));

			List<JobConfig> jobs = JobContext.getContext().getRegistry().getAllJobs();
			info.setJobs(jobs);
			respJson = JsonUtils.toJson(info);
		} else if("POST".equals(req.getMethod())){
			String token = req.getParameter("token");
			TokenGenerator.validate(token, true);
			
			String postJson = CharStreams.toString(new InputStreamReader(req.getInputStream(), "UTF-8"));
			MonitorCommond cmd = JsonUtils.toObject(postJson, MonitorCommond.class);
			cmd.setJobGroup(JobContext.getContext().getGroupName());
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
