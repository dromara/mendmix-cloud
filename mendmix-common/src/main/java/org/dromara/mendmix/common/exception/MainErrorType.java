package org.dromara.mendmix.common.exception;

public enum MainErrorType {

	SERVICE_UNAVAILABLE(503,"error.service.unavailable","服务不可用"),
	DATABASE_ACCESS_ERROR(500,"error.database.error","数据库访问错误"),
	HTTP_METHOD_NOT_ALLOWED(405,"error.unsupport.method","请求方法不支持"),
	FORBIDDEN_REQUEST(403,"error.forbidden","未授权访问"),
	UNAUTHORIZED_REQUEST(401,"error.unauthorized","未登录"),
	UNAUTHORIZED_SYSTEM(403,"error.unauthorized.system","未授权访问该系统"),
	UNAUTHORIZED_DOMAIN(403,"error.unauthorized.domain","未授权域名"),
	SYSTEM_LICENSE_EXPIRED(403,"error.system.license.expired","系统授权过期"),
	TENANT_STATUS_EXPIRED(403,"error.tenant.expired","租户已停用"),
	TOO_MANY_REQUEST_LIMIT(429,"error.request.limit","请求繁忙[E4291]"),
	FALLBACK_REQUEST_LIMIT(429,"error.request.fallabck","请求繁忙[E4292]"),
	BUSINESS_LOGIC_ERROR(500,"error.bizLogic.error","业务处理错误"),
	INVALID_SIGNATURE(400,"error.invalid.signature","签名错误"),
	NOT_SUPPORT(400,"error.nosupported","暂不支持"),
	MISSING_REQUIRED_PARAMETER(400,"error.parameter.missing","参数缺失"),
	UNSUPPORTED_VERSION(400,"error.unsupport.version","不支持该版本号"),
	INVALID_LISENCE(500,"error.invalid.license","lisence异常"),
	INVALID_FORMAT(400,"error.invalid.formart","格式错误"),
	INVALID_CLIENT_ID(400,"error.invalid.clientId","应用ID错误"),
	REPEAT_SUBMIT_ERROR(400,"error.repeat.submit","重复提交"),
	OBJECT_NOT_EXISTS(500,"error.object.notExists","对象不存在"),
	OBJECT_EXISTS(500,"error.object.hasExists","对象已存在"),
	DB_OPTER_FORBIDDEN_ERROR(403,"error.dbOpter.forbidden","禁止当前数据库操作"),
	REMOTE_SERVICE_ERROR(500,"error.remoteService.error","调用远程服务错误");
	
	
	private final int code;
	private final String bizCode;
	private final String message;
	
	private MainErrorType(int code, String bizCode, String message) {
		this.code = code;
		this.bizCode = bizCode;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public String getBizCode() {
		return bizCode;
	}

	public String getMessage() {
		return message;
	}
	
	
	
}
