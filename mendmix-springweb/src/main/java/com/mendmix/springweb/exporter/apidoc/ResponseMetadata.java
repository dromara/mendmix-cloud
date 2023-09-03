/**
 * 
 */
package com.mendmix.springweb.exporter.apidoc;

/**
 * <br>
 * @author 姜维(00770874)
 * @date 2023年7月27日
 */
public class ResponseMetadata {

	private CompositeParameter body;
	private boolean stdResponse = true; //标准响应格式

	public ResponseMetadata() {}
	
	public ResponseMetadata(CompositeParameter body, boolean stdResponse) {
		this.body = body;
		this.stdResponse = stdResponse;
	}

	public CompositeParameter getBody() {
		return body;
	}
	public void setBody(CompositeParameter body) {
		this.body = body;
	}
	public boolean isStdResponse() {
		return stdResponse;
	}
	public void setStdResponse(boolean stdResponse) {
		this.stdResponse = stdResponse;
	}
	
}
