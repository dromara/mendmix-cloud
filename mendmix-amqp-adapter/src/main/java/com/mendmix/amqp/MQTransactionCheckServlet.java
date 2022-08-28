/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.amqp;

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
		boolean status;
		if(transactionChecker == null){
			status = true;
		}else{
			status = transactionChecker.check(request.getParameter(TransactionChecker.TRANSACTION_PARAM_NAME));
		}
		
		PrintWriter out = null;  
		try {  
	        out = response.getWriter();  
	        out.append(String.valueOf(status));  
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
