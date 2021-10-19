package com.jeesuite.springweb.logging;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.WebConstants;
import com.jeesuite.common.async.StandardThreadExecutor;
import com.jeesuite.common.async.StandardThreadExecutor.StandardThreadFactory;
import com.jeesuite.common.http.HttpUtils;
import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.IpUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.springweb.CurrentRuntimeContext;


/**
 * 行为日志采集
 * 
 * <br>
 * Class Name   : RequestLogCollector
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年8月22日
 */
public class RequestLogCollector {
	
	private static Logger log = LoggerFactory.getLogger("request.logger.level");
	
	private static final String ACT_LOG_ADD_URL = ResourceUtils.getProperty("log.push.url");
	private static final String TIMER_TASK = "timerTask";

	private static ThreadLocal<ActionLog> context = new ThreadLocal<>();
	
	private static StandardThreadExecutor asyncSendExecutor;
	
	private static boolean inited = false;
	
	static {
		if(inited = StringUtils.isNotBlank(ACT_LOG_ADD_URL)) {			
			asyncSendExecutor = new StandardThreadExecutor(1, 10,60, TimeUnit.SECONDS, 5000,new StandardThreadFactory("log-asyncSendExecutor"));
		}
	}

	public static ActionLog currentActionLog() {
		return context.get();
	}

	public static void onRequestStart(HttpServletRequest request){
		if(!inited)return;
		ActionLog actionLog = new ActionLog();
		actionLog.setEnv(CurrentRuntimeContext.ENV);
		actionLog.setAppId(CurrentRuntimeContext.APPID);
		actionLog.setRequestAt(new Date());
		actionLog.setRequestIp(IpUtils.getIpAddr(request));
		actionLog.setActionUri(request.getRequestURI());
		actionLog.setOriginAppId(request.getHeader(WebConstants.HEADER_INVOKER_APP_ID));
		String requestId = request.getHeader(WebConstants.HEADER_REQUEST_ID);
		if(StringUtils.isBlank(requestId))requestId = TokenGenerator.generate();
		actionLog.setRequestId(requestId);
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
		if(currentUser != null){
			actionLog.setActionUserId(currentUser.getId());
			actionLog.setActionUserName(currentUser.getUsername());
		}
		
		if(context.get() == null){
			context.set(actionLog);
		}
	}
	
	public static void onBizException(String errorMsg){
		if(context.get() == null)return;
    	ActionLog actionLog = context.get();
    	if(actionLog == null)return;
    	actionLog.setResponseCode(500);
		actionLog.setResponseData(errorMsg);
	}
    
    public static void onResponseEnd(HttpServletResponse response,Throwable throwable){
    	if(context.get() == null) {
    		if(throwable != null) {
    			String requestURI = CurrentRuntimeContext.getRequest().getRequestURI();
    			if (throwable instanceof JeesuiteBaseException) {
    				log.warn("bizError -> request:{},message:{}",requestURI,throwable.getMessage());
    			}else {
    				log.error("systemError",throwable);
    			}
    		}
    		return;
    	}
    	ActionLog actionLog = context.get();
    	if(actionLog == null)return;
    	actionLog.setResponseAt(new Date());
    	actionLog.setResponseCode(response.getStatus());
    	if(throwable != null) {
    		log.error("requestError:{}",actionLog.getExceptions());
		}else  if(log.isDebugEnabled()) {
			String requestLogMessage = RequestLogBuilder.responseLogMessage(actionLog.getResponseCode(), actionLog.getResponseData());
			log.debug(requestLogMessage);
		}
    	try {	
    		asyncSendExecutor.execute(new Runnable() {
				@Override
				public void run() {
					sendUserBehaviorLog(actionLog);
				}
			});
		} catch (Exception e) {
		}finally {
			context.remove();
		}
    }
    
    
    public static void onSystemBackendTaskStart(String taskName,String taskKey){
    	if(!inited)return;
    	ActionLog actionLog = new ActionLog();
    	actionLog.setEnv(CurrentRuntimeContext.ENV);
		actionLog.setAppId(CurrentRuntimeContext.APPID);
		actionLog.setRequestAt(new Date());
		actionLog.setRequestId(TokenGenerator.generate());
		actionLog.setActionName(taskName);
		actionLog.setActionUserName(TIMER_TASK);
		actionLog.setActionUri(taskKey);
		if(context.get() == null){
			context.set(actionLog);
		}
		ThreadContext.put(LogConstants.LOG_CONTEXT_REQUEST_ID, actionLog.getRequestId());
	}
    
    public static void onSystemBackendTaskEnd(Throwable throwable){
    	if(context.get() == null)return;
    	ActionLog actionLog = context.get();
    	if(actionLog == null)return;
    	try {	
    		actionLog.setResponseCode(throwable == null ? 200 : 500);
    		actionLog.setResponseAt(new Date());
    		//send to logserver 
    		asyncSendExecutor.execute(new Runnable() {
				@Override
				public void run() {
					sendUserBehaviorLog(actionLog);
				}
			});
		} catch (Exception e) {
		}finally {
			context.remove();
		}
    }

    private static void sendUserBehaviorLog(ActionLog actionLog){
    	HttpUtils.postJson(ACT_LOG_ADD_URL, JsonUtils.toJson(actionLog));
    }

    public static void destroy(){
    	if(asyncSendExecutor != null) {    		
    		asyncSendExecutor.shutdown();
    	}
    }

}
