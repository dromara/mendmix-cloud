package com.jeesuite.common.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.http.HttpRequestEntity.FileItem;


/**
 *http操作工具类 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年8月24日
 */
public class HttpUtils {

	public static final String DEFAULT_CHARSET = "utf-8";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_GET = "GET";
	public static final String CONTENT_ENCODING_GZIP = "gzip";
	
	private HttpUtils() {
	}
	
	public static HttpResponseEntity get(String requestUri) {
		return get(requestUri, HttpRequestEntity.create());
	}
	
	public static HttpResponseEntity get(String requestUri,HttpRequestEntity requestEntity){
		HttpURLConnection conn = null;
		HttpResponseEntity rsp = null;

		try {
			String ctype = "application/json;charset=" + requestEntity.getCharset();
			String query = buildQuery(requestEntity.getTextParams(), requestEntity.getCharset());
			conn = getConnection(buildGetUrl(requestUri, query), METHOD_GET, ctype,requestEntity);
			rsp = getResponseAsResponseEntity(conn);
		} catch (Exception e) {
			rsp = new HttpResponseEntity(400, e);
		}finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return rsp;
	}
	
	public static HttpResponseEntity postJson(String requestUri,String json,String charset) {
		HttpURLConnection conn = null;
		OutputStream out = null;
		HttpResponseEntity rsp = null;

		try {
			String ctype = "application/json;charset=" + charset;
			conn = getConnection(new URL(requestUri), METHOD_POST, ctype,HttpRequestEntity.create().charset(charset));
			byte[] data = json.getBytes();
			conn.setRequestProperty("Content-Length", String.valueOf(data.length));
			out = conn.getOutputStream();
			out.write(data);
            out.flush();
			rsp = getResponseAsResponseEntity(conn);
		} catch (IOException e) {
			rsp = new HttpResponseEntity(400, e);
		}finally {
			if (out != null) {
				try {out.close();} catch (Exception e2) {}
			}
			if (conn != null) {
				conn.disconnect();
			}
		}

		return rsp;
	}
	
	public static HttpResponseEntity upload(String requestUri,String fieldName,File file){
		return post(requestUri, HttpRequestEntity.create().addFileParam(fieldName, new FileItem(file)));
	}
	
	public static void downloadFile(String fileURL, String saveDir)
            throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();
 
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
 
            if (disposition != null) {
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
            }
            InputStream inputStream = httpConn.getInputStream();
            String saveFilePath = saveDir + File.separator + fileName;
             
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);
 
            int bytesRead = -1;
            byte[] buffer = new byte[2048];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
 
            outputStream.close();
            inputStream.close();
        } else {
        	throw new JeesuiteBaseException(responseCode, "下载失败");
        }
        httpConn.disconnect();
    }
	
	public static HttpResponseEntity post(String requestUri,HttpRequestEntity requestEntity) {
		HttpURLConnection conn = null;
		OutputStream out = null;
		String contextType = null;
		HttpResponseEntity rsp = null;

		try {
			if(requestEntity.getFileParams().isEmpty()){	
				contextType = "application/x-www-form-urlencoded;charset=" + requestEntity.getCharset();
				conn = getConnection(new URL(requestUri), METHOD_POST, contextType,requestEntity);
				out = conn.getOutputStream();
				
				String query = buildQuery(requestEntity.getTextParams(), requestEntity.getCharset());
				byte[] content = {};
				if (query != null) {
					content = query.getBytes(requestEntity.getCharset());
				}
				conn.setRequestProperty("Content-Length", String.valueOf(content.length));
				out.write(content);
			}else{
				String boundary = String.valueOf(System.nanoTime()); // 随机分隔线
				contextType = "multipart/form-data;charset=" + requestEntity.getCharset() + ";boundary=" + boundary;
				conn = getConnection(new URL(requestUri), METHOD_POST, contextType,requestEntity);
				out = conn.getOutputStream();
				
				byte[] entryBoundaryBytes = ("\r\n--" + boundary + "\r\n").getBytes(requestEntity.getCharset());

				// 组装文本请求参数
				Set<Entry<String, String>> textEntrySet = requestEntity.getTextParams().entrySet();
				for (Entry<String, String> textEntry : textEntrySet) {
					byte[] textBytes = getTextEntry(textEntry.getKey(), textEntry.getValue(), requestEntity.getCharset());
					out.write(entryBoundaryBytes);
					out.write(textBytes);
				}

				// 组装文件请求参数
				Set<Entry<String, FileItem>> fileEntrySet = requestEntity.getFileParams().entrySet();
				for (Entry<String, FileItem> fileEntry : fileEntrySet) {
					FileItem fileItem = fileEntry.getValue();
					if (fileItem.getContent() == null) {
						continue;
					}
					byte[] fileBytes = getFileEntry(fileEntry.getKey(), fileItem.getFileName(), fileItem.getMimeType(), requestEntity.getCharset());
					out.write(entryBoundaryBytes);
					out.write(fileBytes);
					out.write(fileItem.getContent());
				}

				// 添加请求结束标志
				byte[] endBoundaryBytes = ("\r\n--" + boundary + "--\r\n").getBytes(requestEntity.getCharset());
				out.write(endBoundaryBytes);
			}
			
			rsp = getResponseAsResponseEntity(conn);
			
		} catch (Exception e) {
			rsp = new HttpResponseEntity(400, e);
		}finally {
			if (out != null) {
				try {out.close();} catch (Exception e2) {}
			}
			if (conn != null) {
				conn.disconnect();
			}
		}
		return rsp;
	}
	
	
	private static HttpURLConnection getConnection(URL url, String method,String contextType, HttpRequestEntity requestEntity) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Accept", "*/*");
		conn.setRequestProperty("Content-Type", contextType);
		conn.setConnectTimeout(requestEntity.getConnectTimeout());
		conn.setReadTimeout(requestEntity.getReadTimeout());
		if (!requestEntity.getHeaders().isEmpty()) {
			for (Map.Entry<String, String> entry : requestEntity.getHeaders().entrySet()) {
				conn.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}
		if(requestEntity.getBasicAuth() != null){
			conn.setRequestProperty("Authorization", requestEntity.getBasicAuth().getEncodeBasicAuth());
		}
		return conn;
	}
	
	private static byte[] getTextEntry(String fieldName, String fieldValue, String charset) throws IOException {
		StringBuilder entry = new StringBuilder();
		entry.append("Content-Disposition:form-data;name=\"");
		entry.append(fieldName);
		entry.append("\"\r\nContent-Type:text/plain\r\n\r\n");
		entry.append(fieldValue);
		return entry.toString().getBytes(charset);
	}

	private static byte[] getFileEntry(String fieldName, String fileName, String mimeType, String charset) throws IOException {
		StringBuilder entry = new StringBuilder();
		entry.append("Content-Disposition:form-data;name=\"");
		entry.append(fieldName);
		entry.append("\";filename=\"");
		entry.append(fileName);
		entry.append("\"\r\nContent-Type:");
		entry.append(mimeType);
		entry.append("\r\n\r\n");
		return entry.toString().getBytes(charset);
	}
	
	private static URL buildGetUrl(String strUrl, String query) throws IOException {
		URL url = new URL(strUrl);
		if (StringUtils.isEmpty(query)) {
			return url;
		}

		if (StringUtils.isEmpty(url.getQuery())) {
			if (strUrl.endsWith("?")) {
				strUrl = strUrl + query;
			} else {
				strUrl = strUrl + "?" + query;
			}
		} else {
			if (strUrl.endsWith("&")) {
				strUrl = strUrl + query;
			} else {
				strUrl = strUrl + "&" + query;
			}
		}

		return new URL(strUrl);
	}

	public static String buildQuery(Map<String, String> params, String charset) throws IOException {
		if (params == null || params.isEmpty()) {
			return null;
		}

		StringBuilder query = new StringBuilder();
		Set<Entry<String, String>> entries = params.entrySet();
		boolean hasParam = false;

		for (Entry<String, String> entry : entries) {
			String name = entry.getKey();
			String value = entry.getValue();
			// 忽略参数名或参数值为空的参数
			if (StringUtils.isAnyEmpty(name, value)) {
				if (hasParam) {
					query.append("&");
				} else {
					hasParam = true;
				}

				query.append(name).append("=").append(URLEncoder.encode(value, charset));
			}
		}

		return query.toString();
	}

	private static HttpResponseEntity getResponseAsResponseEntity(HttpURLConnection conn) throws IOException {
		HttpResponseEntity responseEntity = new HttpResponseEntity();
		String charset = getResponseCharset(conn.getContentType());
		InputStream es = conn.getErrorStream();
		
		responseEntity.setStatusCode(conn.getResponseCode());
		if (es == null) {
			String contentEncoding = conn.getContentEncoding();
			if (CONTENT_ENCODING_GZIP.equalsIgnoreCase(contentEncoding)) {
				responseEntity.setBody(getStreamAsString(new GZIPInputStream(conn.getInputStream()), charset));
			} else {
				responseEntity.setBody(getStreamAsString(conn.getInputStream(), charset));
			}
		} else {
			String msg = getStreamAsString(es, charset);
			if (StringUtils.isEmpty(msg)) {
				responseEntity.setBody(conn.getResponseCode() + ":" + conn.getResponseMessage());
			} else {
				responseEntity.setBody(msg);
			}
		}
		
		return responseEntity;
	}

	private static String getStreamAsString(InputStream stream, String charset) throws IOException {
		try {
			Reader reader = new InputStreamReader(stream, charset);
			StringBuilder response = new StringBuilder();

			final char[] buff = new char[1024];
			int read = 0;
			while ((read = reader.read(buff)) > 0) {
				response.append(buff, 0, read);
			}

			return response.toString();
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	private static String getResponseCharset(String ctype) {
		String charset = DEFAULT_CHARSET;

		if (!StringUtils.isEmpty(ctype)) {
			String[] params = ctype.split(";");
			for (String param : params) {
				param = param.trim();
				if (param.startsWith("charset")) {
					String[] pair = param.split("=", 2);
					if (pair.length == 2) {
						if (!StringUtils.isEmpty(pair[1])) {
							charset = pair[1].trim();
						}
					}
					break;
				}
			}
		}

		return charset;
	}
	
	public static void main(String[] args) throws IOException {
		HttpRequestEntity entity = HttpRequestEntity.create()
				 .addFileParam("file", new FileItem("/Users/jiangwei/333.log"))
				 .basicAuth("admin", "123456");
		System.out.println(post("http://192.168.1.89:9082/", entity));
	}

}
