package org.dromara.mendmix.springweb.exporter.apidoc;

import java.util.ArrayList;
import java.util.List;


/**
 * 
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年7月27日
 */
public class RequestMetadata {

	private List<SimpleParameter> pathParameters;
	private List<SimpleParameter> headers;
	private List<SimpleParameter> queryParameters;
	private CompositeParameter body;
	
	
	public List<SimpleParameter> getPathParameters() {
		return pathParameters;
	}
	public void setPathParameters(List<SimpleParameter> pathParameters) {
		this.pathParameters = pathParameters;
	}
	public List<SimpleParameter> getHeaders() {
		return headers;
	}
	public void setHeaders(List<SimpleParameter> headers) {
		this.headers = headers;
	}
	public List<SimpleParameter> getQueryParameters() {
		return queryParameters;
	}
	public void setQueryParameters(List<SimpleParameter> queryParameters) {
		this.queryParameters = queryParameters;
	}
	public CompositeParameter getBody() {
		return body;
	}
	public void setBody(CompositeParameter body) {
		this.body = body;
	}
	
	public void addQueryParameter(SimpleParameter parameter) {
		if(queryParameters == null)queryParameters = new ArrayList<>(5);
		queryParameters.add(parameter);
	}
	
	public void addPathParameter(SimpleParameter parameter) {
		if(pathParameters == null)pathParameters = new ArrayList<>(5);
		pathParameters.add(parameter);
	}
}
