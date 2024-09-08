/**
 * 
 */
package org.dromara.mendmix.logging.tracelog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.util.TimeConvertUtils;
import org.dromara.mendmix.logging.LogConfigs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <br>
 * @author vakinge
 * @date 2024年2月26日
 */
@JsonInclude(Include.NON_NULL)
public class TraceSpan implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String traceId;
	private String id;
	private String pid;
	private String env;
	private String systemId;
	private String serviceId;
	private String traceType;
	private String traceMethod;
	private String traceKey; //sql/请求地址/缓存key
	private Date startTime;
	private Date endTime;
	private boolean successed;
	private String exceptions;
	@JsonIgnore
	private TraceSpan parent;
	private List<TraceSpan> children;
	
	@JsonProperty("@timestamp")
	private long timestamp;
	
	private int timeOffset;
	
	private int useTime;

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getEnv() {
		return env;
	}

	public void setEnv(String env) {
		this.env = env;
	}

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getTraceType() {
		return traceType;
	}

	public void setTraceType(String traceType) {
		this.traceType = traceType;
	}

	public String getTraceMethod() {
		return traceMethod;
	}

	public void setTraceMethod(String traceMethod) {
		this.traceMethod = traceMethod;
	}

	public String getTraceKey() {
		return traceKey;
	}

	public void setTraceKey(String traceKey) {
		this.traceKey = traceKey;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	
	public boolean isSuccessed() {
		return successed;
	}

	public void setSuccessed(boolean successed) {
		this.successed = successed;
	}

	public String getExceptions() {
		return exceptions;
	}

	public void setExceptions(String exceptions) {
		this.exceptions = exceptions;
	}
	
	public int getTimeOffset() {
		return timeOffset;
	}

	public void setTimeOffset(int timeOffset) {
		this.timeOffset = timeOffset;
	}

	public TraceSpan getParent() {
		return parent;
	}

	public void setParent(TraceSpan parent) {
		this.parent = parent;
		this.pid = parent.getId();
	}

	public List<TraceSpan> getChildren() {
		return children;
	}

	public void setChildren(List<TraceSpan> children) {
		this.children = children;
	}

	public void addChild(TraceSpan span) {
		if(children == null)children = new ArrayList<>(5);
		span.setParent(this);
		children.add(span);
	}
	
	public void end() {
		if(this.endTime != null)return;
		this.endTime = new Date();
		this.useTime = (int) (this.endTime.getTime() - this.startTime.getTime());
		this.successed = true;
	}
	
	public void endWithException(String exceptions) {
		end();
		this.successed = false;
		this.exceptions = exceptions;
	}
	
	public int getUseTime() {
		return useTime;
	}

	public void setUseTime(int useTime) {
		this.useTime = useTime;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public static TraceSpan buildTraceSpan(TraceType traceType, String method, String traceKey) {
		TraceSpan traceLog = new TraceSpan();
		traceLog.setId(UUID.randomUUID().toString().replace(GlobalConstants.MID_LINE, StringUtils.EMPTY));
		traceLog.setTraceId(CurrentRuntimeContext.getRequestId());
		traceLog.setEnv(GlobalContext.ENV.toUpperCase());
		traceLog.setSystemId(GlobalContext.SYSTEM_KEY);
		traceLog.setServiceId(GlobalContext.APPID);
		traceLog.setTraceType(traceType.name());
		traceLog.setTraceMethod(method);
		traceLog.setTraceKey(traceKey);
		traceLog.setStartTime(new Date());
		traceLog.setTimestamp(traceLog.getStartTime().getTime());
		traceLog.setTimeOffset(TimeConvertUtils.TIME_ZONE_OFFSET);
		return traceLog;
	}
	
	public List<TraceSpan> toTileList(){
		if(this.children == null || this.children.isEmpty()) {
			return Arrays.asList(this);
		}
		List<TraceSpan> list = new ArrayList<>();
		toTileList(this, list);
		if(LogConfigs.TRACE_LOGGING_TIME_THRESHOLD <= 0)return list;
		return list.stream() //
				.filter(o -> o.getUseTime() > LogConfigs.TRACE_LOGGING_TIME_THRESHOLD)
				.sorted(Comparator.comparing(TraceSpan::getStartTime)) //
				.collect(Collectors.toList());
	}
	
    private static void toTileList(TraceSpan span,List<TraceSpan> list){
    	List<TraceSpan> children = span.getChildren();
		if(children != null) {
			span.setChildren(null);
		}
		list.add(span);
		if(children != null) {
			for (TraceSpan child : children) {
				toTileList(child, list);
			}
		}
	}

	@Override
	public String toString() {
		return "[traceId=" + traceId + ", serviceId=" + serviceId + ", traceType=" + traceType
				+ ", traceMethod=" + traceMethod + ", traceKey=" + traceKey + ", startTime=" + startTime + "]";
	}
    
	
}
