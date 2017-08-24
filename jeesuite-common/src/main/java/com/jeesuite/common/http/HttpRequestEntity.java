package com.jeesuite.common.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.jeesuite.common.crypt.Base64;

public class HttpRequestEntity {

	private static final String DEFAULT_CHARSET = "utf-8";
	private String charset = DEFAULT_CHARSET;
	private int connectTimeout = 5000;
	private int readTimeout = 10000;
	private Map<String, String> headers = new HashMap<>();
	private Map<String, String> textParams = new HashMap<>();
	private Map<String, FileItem> fileParams = new HashMap<>();
	private BasicAuthParams basicAuth;
	
	
	private HttpRequestEntity() {}

	public static HttpRequestEntity create(){
		return new HttpRequestEntity();
	}
	
	public String getCharset() {
		return charset;
	}

	public HttpRequestEntity charset(String charset) {
		this.charset = charset;
		return this;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public HttpRequestEntity connectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		return this;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public HttpRequestEntity readTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
		return this;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public HttpRequestEntity headers(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	public Map<String, String> getTextParams() {
		return textParams;
	}

	public HttpRequestEntity textParams(Map<String, String> textParams) {
		this.textParams = textParams;
		return this;
	}

	public Map<String, FileItem> getFileParams() {
		return fileParams;
	}

	public HttpRequestEntity fileParams(Map<String, FileItem> fileParams) {
		this.fileParams = fileParams;
		return this;
	}

	public BasicAuthParams getBasicAuth() {
		return basicAuth;
	}

	public HttpRequestEntity basicAuth(String userName,String password) {
		this.basicAuth = new BasicAuthParams(userName, password);
		return this;
	}
	
	public HttpRequestEntity addHeader(String name,String value) {
		this.headers.put(name, value);
		return this;
	}
	
	public HttpRequestEntity addTextParam(String name,String value) {
		this.textParams.put(name, value);
		return this;
	}
	
	public HttpRequestEntity addFileParam(String name,FileItem value) {
		this.fileParams.put(name, value);
		return this;
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
				this.mimeType = getMimeType(getContent());
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
		
		/**
		 * 获取文件的真实媒体类型。目前只支持JPG, GIF, PNG, BMP四种图片文件。
		 * 
		 * @param bytes 文件字节流
		 * @return 媒体类型(MEME-TYPE)
		 */
		public static String getMimeType(byte[] bytes) {
			String suffix = getFileSuffix(bytes);
			String mimeType;

			if ("JPG".equals(suffix)) {
				mimeType = "image/jpeg";
			} else if ("GIF".equals(suffix)) {
				mimeType = "image/gif";
			} else if ("PNG".equals(suffix)) {
				mimeType = "image/png";
			} else if ("BMP".equals(suffix)) {
				mimeType = "image/bmp";
			}else {
				mimeType = "application/octet-stream";
			}

			return mimeType;
		}
		
		/**
		 * 获取文件的真实后缀名。目前只支持JPG, GIF, PNG, BMP四种图片文件。
		 * 
		 * @param bytes 文件字节流
		 * @return JPG, GIF, PNG or null
		 */
		public static String getFileSuffix(byte[] bytes) {
			if (bytes == null || bytes.length < 10) {
				return null;
			}

			if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
				return "GIF";
			} else if (bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
				return "PNG";
			} else if (bytes[6] == 'J' && bytes[7] == 'F' && bytes[8] == 'I' && bytes[9] == 'F') {
				return "JPG";
			} else if (bytes[0] == 'B' && bytes[1] == 'M') {
				return "BMP";
			} else {
				return null;
			}
		}

	}
}
