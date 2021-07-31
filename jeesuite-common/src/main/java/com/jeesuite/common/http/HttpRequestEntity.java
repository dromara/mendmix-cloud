package com.jeesuite.common.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.crypt.Base64;
import com.jeesuite.common.util.MimeTypeUtils;

/**
 * 
 * 
 * <br>
 * Class Name   : HttpRequestEntity
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年8月24日
 */
public class HttpRequestEntity {

	private HttpMethod method;
	private String charset;
	private String contentType;
	private Map<String, String> headers;
	private Map<String, Object> queryParams;
	private Map<String, Object> formParams;
	private String body;
	private BasicAuthParams basicAuth;
	
	private boolean  multipart;
	private String boundary;
	
	private HttpRequestEntity() {}

	public static HttpRequestEntity create(HttpMethod method){
		return new HttpRequestEntity().method(method);
	}
	
	

	public String getCharset() {
		if(charset == null) {
			charset = parseContentTypeCharset(contentType);
		}
		return charset;
	}

	public HttpRequestEntity charset(String charset) {
		this.charset = charset;
		return this;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public HttpRequestEntity method(HttpMethod method) {
		this.method = method;
		return this;
	}

	public String getContentType() {
		if(StringUtils.isBlank(contentType)) {
			return HttpClientProvider.CONTENT_TYPE_JSON_UTF8;
		}
		return contentType;
	}

	public HttpRequestEntity contentType(String contentType) {
		this.contentType = contentType;
		return this;
	}
	
	public boolean isMultipart() {
		return multipart;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public HttpRequestEntity headers(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}
	
	public HttpRequestEntity header(String name,String value) {
		if(this.headers == null)this.headers = new HashMap<>(3);
		this.headers.put(name, value);
		return this;
	}

	public Map<String, Object> getQueryParams() {
		return queryParams;
	}

	public HttpRequestEntity queryParams(Map<String, Object> queryParams) {
		this.queryParams = queryParams;
		return this;
	}
	
	public HttpRequestEntity queryParam(String name,Object value) {
		if(this.queryParams == null)this.queryParams = new HashMap<>(3);
		this.queryParams.put(name, value);
		return this;
	}

	
	public Map<String, Object> getFormParams() {
		return formParams;
	}
	
	public HttpRequestEntity fileParam(String name,File file) {
		if(this.formParams == null)this.formParams = new HashMap<>();
		this.formParams.put(name, new FileItem(file));
		if(contentType == null) {
			contentType = HttpClientProvider.CONTENT_TYPE_FROM_MULTIPART_UTF8;
		}
		if(!multipart) {
			multipart = true;
			boundary = String.valueOf(System.nanoTime()); // 随机分隔线
			contentType = contentType + ";boundary=" + boundary;
		}
		return this;
	}

	public HttpRequestEntity formParam(String name,String value) {
		if(this.formParams == null)this.formParams = new HashMap<>();
		this.formParams.put(name, value);
		if(contentType == null) {
			contentType = HttpClientProvider.CONTENT_TYPE_FROM_URLENCODED_UTF8;
		}
		return this;
	}
	
	public String getBody() {
		return body;
	}

	public HttpRequestEntity body(String body) {
        if(method != HttpMethod.POST) {
			return null;
		}
		this.body = body;
		return this;
	}
	
	public String getBoundary() {
		return boundary;
	}

	public BasicAuthParams getBasicAuth() {
		return basicAuth;
	}

	public HttpRequestEntity basicAuth(String userName,String password) {
		this.basicAuth = new BasicAuthParams(userName, password);
		return this;
	}
	
	
	public static String parseContentTypeCharset(String contentType) {
		String charset = HttpClientProvider.CHARSET_UTF8;
		if(StringUtils.isBlank(contentType))return charset;
		String[] params = StringUtils.split(contentType, ";");
		for (String param : params) {
			param = param.trim();
			if (param.toLowerCase().startsWith("charset")) {
				String[] pair = param.split("=", 2);
				if (pair.length == 2) {
					if (!StringUtils.isEmpty(pair[1])) {
						charset = pair[1].trim();
					}
				}
				break;
			}
		}

		return charset;
	}

	public static class BasicAuthParams{
		private String name;
		private String password;
		
		public BasicAuthParams(String name, String password) {
			super();
			this.name = name;
			this.password = password;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		
		public String getEncodeBasicAuth(){
			String encoded = Base64.encodeToString((name + ":" + password).getBytes(StandardCharsets.UTF_8),false);
			return "Basic "+encoded;
		}
		
	}
	
	/**
	 * 文件元数据。
	 */
	public static class FileItem {

		private String fileName;
		private String mimeType;
		private byte[] content;
		private File file;

		/**
		 * 基于本地文件的构造器。
		 * 
		 * @param file 本地文件
		 */
		public FileItem(File file) {
			this.file = file;
		}

		/**
		 * 基于文件绝对路径的构造器。
		 * 
		 * @param filePath 文件绝对路径
		 */
		public FileItem(String filePath) {
			this(new File(filePath));
		}

		/**
		 * 基于文件名和字节流的构造器。
		 * 
		 * @param fileName 文件名
		 * @param content 文件字节流
		 */
		public FileItem(String fileName, byte[] content) {
			this.fileName = fileName;
			this.content = content;
		}

		/**
		 * 基于文件名、字节流和媒体类型的构造器。
		 * 
		 * @param fileName 文件名
		 * @param content 文件字节流
		 * @param mimeType 媒体类型
		 */
		public FileItem(String fileName, byte[] content, String mimeType) {
			this(fileName, content);
			this.mimeType = mimeType;
		}

		public String getFileName() {
			if (this.fileName == null && this.file != null && this.file.exists()) {
				this.fileName = file.getName();
			}
			return this.fileName;
		}

		public String getMimeType() throws IOException {
			if (this.mimeType == null) {
				this.mimeType = MimeTypeUtils.getMimeType(getContent()).getMimeType();
			}
			return this.mimeType;
		}

		public byte[] getContent() throws IOException {
			if (this.content == null && this.file != null && this.file.exists()) {
				InputStream in = null;
				ByteArrayOutputStream out = null;

				try {
					in = new FileInputStream(this.file);
					out = new ByteArrayOutputStream();
					int ch;
					while ((ch = in.read()) != -1) {
						out.write(ch);
					}
					this.content = out.toByteArray();
				} finally {
					if (out != null) {
						out.close();
					}
					if (in != null) {
						in.close();
					}
				}
			}
			return this.content;
		}
		
	}
}
