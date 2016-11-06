/**
 * 
 */
package com.jeesuite.monitor.web.controller;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.jeesuite.monitor.model.App;
import com.jeesuite.monitor.model.AppConfig;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月3日
 */
public class ConfigCenterController extends BaseController {

	public void upload() {
		try {
			String fileName = null;
			StringBuilder content = new StringBuilder();
			List<String> lines = IOUtils.readLines(getRequest().getInputStream(), StandardCharsets.UTF_8);
			for (String line : lines) {
				if (StringUtils.isBlank(line))
					continue;
				if (line.contains("--"))
					continue;
				if (line.contains("filename=")) {
					fileName = line.split("filename=")[1].replace("\"", "");
					continue;
				}
				if (line.startsWith("Content-"))
					continue;
				if (fileName == null)
					continue;
				content.append(line).append("\n");
			}
			Map<String, Object> obj = new HashMap<String, Object>();
			obj.put("status", 1);
			obj.put("fileName", fileName);
			obj.put("content", content.toString());
			renderJson(obj);
		} catch (Exception e) {
			ajaxError("上传失败");
		}
	}

	public void check() {

	}

	public void download() {
		String app = getPara("app");
		String env = getPara("env");
		String version = getPara("ver", "0.0.0");
		String fileName = getPara("file");

		AppConfig config = AppConfig.dao.findFirst("select * from app_configs where app_name = ? and version = ? and env = ? and `name`=?", app, version,env,fileName);

		if (config == null) {
			renderText("not_found");
			return;
		}

		OutputStream output = null;
		try {
			String content = config.getContents();
			getResponse().addHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes()));
			getResponse().addHeader("Content-Length", "" + content.length());
			output = new BufferedOutputStream(getResponse().getOutputStream());
			getResponse().setContentType("application/octet-stream");
			byte[] bytes = content.getBytes();
			output.write(bytes);
			output.flush();
			renderNull();
		} catch (Exception e) {
			renderText("download_error");
		}finally {			
			if(output != null){
				try {output.close(); } catch (Exception e) {}
			}
		}

	}

	public void queryconf() {
		String app = getPara("app");
		if (StringUtils.isNotBlank(app)) {
			String env = getPara("env", "dev");
			String version = getPara("ver", "0.0.0");
			List<AppConfig> list = AppConfig.dao
					.find("select * from app_configs where app_name=? and version=? and env=?", app, version, env);

			if (list != null)
				setAttr("configs", list);
			//
			setAttr("app", app);
			setAttr("env", env);
			setAttr("ver", version);
		}

		setAttr("apps", App.dao.find("select * from apps"));
		render("queryconf.html");
	}

	public void addapp() {
		if (!isAjax()) {
			render("addapp.html");
			return;
		}
		App app = new App();
		String name = getPara("name");
		if (App.dao.findById(name) != null) {
			ajaxError("该项目名称已存在");
			return;
		}
		app.setName(name);
		app.setRemarks(getPara("remarks"));
		app.setNotifyEmails(getPara("notify_emails"));
		app.save();
		ajaxSuccess("添加成功");
	}

	public void addconf() {
		if (!isAjax()) {
			setAttr("apps", App.dao.find("select * from apps"));
			render("addconf.html");
			return;
		}

		boolean isGlobal = getParaToInt("is_global",0) == 1;
		String app = isGlobal ? "global" : getPara("app_name");
		if(StringUtils.isBlank(app)){
			ajaxError("请选择应用");
			return;
		}
		String ver = getPara("version");
		String env = getPara("env");
		String file = getPara("file_name");

		AppConfig config = AppConfig.dao.findFirst(
				"select * from app_configs where app_name = ? and version = ? and env = ? and name = ?", app, ver, env,
				file);
		if (config != null) {
			ajaxError("该配置文件已存在");
			return;
		}
		AppConfig appConfig = new AppConfig();
		appConfig.setAppName(app);
		appConfig.setEnv(env);
		appConfig.setVersion(ver);
		appConfig.setName(file);
		appConfig.setType(1);
		appConfig.setContents(getPara("contents"));
		appConfig.setCreatedAt(new Date());
		appConfig.setUpdatedAt(appConfig.getCreatedAt());
		appConfig.save();
		ajaxSuccess();
	}

}
