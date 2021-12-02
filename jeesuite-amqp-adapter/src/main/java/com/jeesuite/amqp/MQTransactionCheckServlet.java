package com.jeesuite.amqp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * <br>
 * Class Name   : MQTransactionCheckServlet
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年1月13日
 */
public class MQTransactionCheckServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	public static final String URI = "/mqtc/checker";
	
	private TransactionChecker transactionChecker;
	
	public MQTransactionCheckServlet(TransactionChecker transactionChecker) {
		super();
		this.transactionChecker = transactionChecker;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		MessageStatus status;
		if(transactionChecker == null){
			status = MessageStatus.unprocessed;
		}else{
			status = transactionChecker.check(request.getParameter(TransactionChecker.TRANSACTION_PARAM_NAME));
		}
		
		PrintWriter out = null;  
		try {  
	        out = response.getWriter();  
	        out.append(status.name());  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    } finally {  
	        if (out != null) {  
	            out.close();  
	        }  
	    }  
	}

	@Override
	public void destroy() {
		super.destroy();
	}
}
