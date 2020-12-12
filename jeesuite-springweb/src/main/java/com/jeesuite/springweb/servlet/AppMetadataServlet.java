/**
 * 
 */
package com.jeesuite.springweb.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.springweb.AppMetadataHolder;
import com.jeesuite.springweb.model.WrapperResponse;
import com.jeesuite.springweb.utils.WebUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年4月17日
 */
@WebServlet(urlPatterns = "/metadata", description = "应用信息")
public class AppMetadataServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
private static String metadataJSON;
	
	
	@Override
	public void init() throws ServletException {
		super.init();
		metadataJSON = JsonUtils.toJson(AppMetadataHolder.getMetadata());
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if(!WebUtils.isInternalRequest(req)){
			WebUtils.responseOutJson(resp, JsonUtils.toJson(new WrapperResponse<>(403, "外网禁止访问")));
			return;
		}
		WebUtils.responseOutJson(resp, metadataJSON);
	}

	@Override
	public void destroy() {
		super.destroy();
	}

}
