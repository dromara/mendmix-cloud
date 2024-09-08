package org.dromara.mendmix.amqp;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dromara.mendmix.spring.InstanceFactory;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年8月15日
 */
//@WebServlet(urlPatterns = "/messagestate/check", description = "消息状态检查")
public class MessageStateCheckEndpoint extends HttpServlet{

	private static final long serialVersionUID = 1L;
	
	private MessageStateCheckService messageStateCheckService;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		boolean result = false;
		try {
			String txId = req.getParameter("txId");
			result = messageStateCheckService.checkMessageTx(txId);
			resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
			resp.setContentType("text/plain; charset=utf-8");
		} catch (Exception e) {}
		
		PrintWriter out = null;
		try {
			out = resp.getWriter();
			out.append(String.valueOf(result));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		messageStateCheckService = InstanceFactory.getInstance(MessageStateCheckService.class);
	}
	
	

}
