/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.jeesuite.common.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.GlobalConstants;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.crypt.Base64;
import com.jeesuite.common.util.BeanUtils;
import com.jeesuite.common.util.MimeTypeUtils;
import com.jeesuite.common.util.MimeTypeUtils.FileMeta;
import com.jeesuite.common.util.ResourceUtils;
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
	
	public HttpRequestEntity backendInternalCall() {
		header(CustomRequestHeaders.HEADER_RESP_KEEP, Boolean.TRUE.toString());
		header(CustomRequestHeaders.HEADER_IGNORE_TENANT, Boolean.TRUE.toString());
		header(CustomRequestHeaders.HEADER_IGNORE_AUTH, Boolean.TRUE.toString());
		return header(CustomRequestHeaders.HEADER_INTERNAL_REQUEST, Boolean.TRUE.toString());
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
	
	public HttpRequestEntity fileParam(String name,String originalFilename,InputStream inputStream, String mimeType,  long size) {
		if(this.formParams == null)this.formParams = new HashMap<>();
		this.formParams.put(name, new FileItem(originalFilename,inputStream, mimeType, size));
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
	
	public HttpRequestEntity formParams(Object formdata) {
		if (formdata instanceof Map) {
			this.formParams = (Map<String, Object>) formdata;
		} else {
			this.formParams = BeanUtils.beanToMap(formdata);
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
	
	public void unset() {
		if(!isMultipart())return;
		Object entryValue;
		for (Entry<String, Object> entry : formParams.entrySet()) {
			entryValue = entry.getValue();
			if(entryValue == null)continue;
			if(entryValue instanceof FileItem) {
				FileItem fileItem = (FileItem) entryValue;
				if(fileItem.content != null) {
					fileItem.content = null;
				}else if(fileItem.createdTempFile) {
					try {
						FileUtils.forceDelete(fileItem.file);
						fileItem.createdTempFile = false;
					} catch (Exception e) {}
				}
			}
		}
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
		//每块限制大小，超过的要分块上传
		private static int chunkSizeLimit = ResourceUtils.getInt("application.httputil.fileupload.chunkSizeLimit", 1024 * 1024);
		private static String tmpDir = System.getProperty("java.io.tmpdir") + File.separator + "jeesuite";
		static {
			File dir = new File(tmpDir);
			if(!dir.exists()) {
				dir.mkdirs();
			}
		}
		private String fileName;
		private String mimeType;
		private byte[] content;
		private File file;
		private InputStream inputStream;
		private long size;
		
		private int chunkNum = 1;
		
		private long offset;
		private boolean createdTempFile;

		/**
		 * 基于本地文件的构造器。
		 * 
		 * @param file 本地文件
		 */
		public FileItem(File file) {
			this.file = file;
			this.fileName = file.getName();
			if(file.getName().contains(GlobalConstants.DOT)) {
				String extension = fileName.substring(fileName.lastIndexOf(GlobalConstants.DOT) + 1).toLowerCase();
				this.mimeType = MimeTypeUtils.getFileMimeType(extension);
			}
			setSize(file.length());
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
			this.size = content.length;
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
		
		

		public FileItem(String originalFilename,InputStream inputStream, String mimeType,  long size) {
			this.fileName = originalFilename;
			this.mimeType = mimeType;
			setSize(size);
			//写入临时文件
			if(chunkNum > 1) {
				String ext = null;
				if(originalFilename.contains(GlobalConstants.DOT)) {
					ext = originalFilename.substring(originalFilename.lastIndexOf(GlobalConstants.DOT));
				}else {
					ext = GlobalConstants.DOT + StringUtils.defaultIfBlank(MimeTypeUtils.getFileExtension(mimeType), "tmp");
				}
				
				this.file = new File(tmpDir, UUID.randomUUID().toString() + ext);
               
				FileOutputStream outputStream = null;
				try {
					outputStream = new FileOutputStream(file);
					IOUtils.copy(inputStream, outputStream);
					createdTempFile = true;
				} catch (Exception e) {
					throw new JeesuiteBaseException("写入临时文件错误:" + e.getMessage());
				}finally {
					try {if(outputStream != null)outputStream.close();} catch (Exception e2) {}
				}
			}else {
				this.inputStream = inputStream;
			}
		}

		public String getFileName() {
			if(this.fileName == null) {
				this.fileName = "1.txt";
			}
			return this.fileName;
		}

		public String getMimeType() throws IOException {
			if (this.mimeType == null) {
				FileMeta fileMeta = MimeTypeUtils.getFileMeta(getContent());
				if(fileMeta != null) {
					return fileMeta.getMimeType();
				}
			}
			return this.mimeType;
		}
		

		public InputStream getInputStream() {
			return inputStream;
		}

		public long getSize() {
			return size;
		}
		
		public int getChunkNum() {
			return chunkNum;
		}

		private void setSize(long size) {
			this.size = size;
			if(chunkSizeLimit > 0 && size > chunkSizeLimit) {
				chunkNum = (int) (size / chunkSizeLimit);
				if((size / chunkSizeLimit) > 0) {
					chunkNum = chunkNum + 1;
				}
			}
		}
		

		public byte[] getContent() throws IOException {
			if(chunkNum > 1) {
				if(offset >= size)return new byte[0];
				byte[] data = getFileChunk(file, offset, chunkSizeLimit);
				offset = offset + chunkSizeLimit;
				if(offset > size)offset = size;
				return data;
			}
			if(this.content != null)return this.content;
			if(inputStream == null && this.file != null && this.file.exists()) {
				inputStream = new FileInputStream(this.file);
			}
			if(inputStream == null)return null;
			ByteArrayOutputStream out = null;

			try {
				out = new ByteArrayOutputStream();
				int ch;
				while ((ch = inputStream.read()) != -1) {
					out.write(ch);
				}
				this.content = out.toByteArray();
			} finally {
				if (out != null) {
					out.close();
				}
				if (inputStream != null) {
					inputStream.close();
					inputStream = null;
				}
			}
			return this.content;
		}
		
		/**
	     * 文件分块工具
	     * @param file 文件
	     * @param offset 起始偏移位置
	     * @param chunkSize 分块大小
	     * @return 分块数据
	     */
	    private static byte[] getFileChunk(File file, long offset, int chunkSize) {

	        byte[] result = new byte[chunkSize];
	        RandomAccessFile accessFile = null;
	        try {
	            accessFile = new RandomAccessFile(file, "r");
	            accessFile.seek(offset);
	            int readSize = accessFile.read(result);
	            if (readSize == -1) {
	                return null;
	            } else if (readSize == chunkSize) {
	                return result;
	            } else {
	                byte[] tmpByte = new byte[readSize];
	                System.arraycopy(result, 0, tmpByte, 0, readSize);
	                return tmpByte;
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        } finally {
	        	if (accessFile != null) {
	                try {accessFile.close();} catch (IOException e1) {}
	            }
	        }
	        return null;
	    }
		
	}
}
