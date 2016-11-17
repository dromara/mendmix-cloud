/**
 * 
 */
package com.jeesuite.monitor.web;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import com.jfinal.core.JFinalFilter;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月7日
 */
public class WebServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int port = 8080;
		EnumSet<DispatcherType> all = EnumSet.of(DispatcherType.ASYNC, DispatcherType.ERROR, 
    			DispatcherType.FORWARD,DispatcherType.INCLUDE, DispatcherType.REQUEST);
        try{
        	final Server server = new Server();
        	SelectChannelConnector connector = new SelectChannelConnector();
    		connector.setPort(port);
    		server.addConnector(connector);
            WebAppContext context = new WebAppContext("webapp","/");
            FilterHolder filter = new FilterHolder(new JFinalFilter());
            filter.setInitParameter("configClass", WebAppConfig.class.getName());
            context.addFilter(filter, "/kafka/*", all);
            context.addFilter(filter, "/scheduler/*", all);
            context.addFilter(filter, "/confcenter/*", all);
            context.addFilter(filter, "/auth/*", all);
            server.setHandler(context);
            server.start();
            server.join();
        }catch (Exception e){
            e.printStackTrace();
        }
		//JFinal.start("webapp", 8080, "/", 3600);
	}

}
