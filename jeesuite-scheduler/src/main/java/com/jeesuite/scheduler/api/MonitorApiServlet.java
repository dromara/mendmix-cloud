/**
 * 
 */
package com.jeesuite.scheduler.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.scheduler.JobContext;
import com.jeesuite.scheduler.model.JobConfig;
import com.jeesuite.scheduler.model.JobGroupInfo;


@WebServlet(urlPatterns = "/scheduler/status", description = "定时任务监控")
public class MonitorApiServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		JobGroupInfo info = new JobGroupInfo();
		info.setName(JobContext.getContext().getGroupName());
		info.setClusterNodes(new ArrayList<>(JobContext.getContext().getActiveNodes()));

		List<JobConfig> jobs = JobContext.getContext().getRegistry().getAllJobs();
		info.setJobs(jobs);

		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.setContentType("application/json; charset=utf-8");
		PrintWriter out = null;
		try {
			out = resp.getWriter();
			out.append(JsonUtils.toJson(info));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.close();
			}
		}

	}

}
