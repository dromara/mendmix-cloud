package org.dromara.mendmix.common.http;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLHelper {

	private static SSLContext sslContext;
	private static HostnameVerifier hostnameVerifier;
	
	public static SSLContext getSslContext() throws Exception {
		if(sslContext == null) {
			TrustManager[] trustAllCerts = new TrustManager[1];
			TrustManager tm = new CustomTrustManager();
			trustAllCerts[0] = tm;
			sslContext = SSLContext.getInstance(HttpClientProvider.sslCipherSuites);
			sslContext.init(null, trustAllCerts, null);
		}
		return sslContext;
	}

	public static HostnameVerifier getHostnameVerifier() {
		if(hostnameVerifier == null) {
			hostnameVerifier = new HostnameVerifier() {
				public boolean verify(String urlHostName, SSLSession session) {
					return true;
				}
			};
		}
		return hostnameVerifier;
	}

	static class CustomTrustManager implements X509TrustManager {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
		public boolean isServerTrusted(X509Certificate[] certs) {
			return true;
		}
		public boolean isClientTrusted(X509Certificate[] certs) {
			return true;
		}
		public void checkServerTrusted(X509Certificate[] certs, String authType)
				throws CertificateException {
			return;
		}
		public void checkClientTrusted(X509Certificate[] certs, String authType)
				throws CertificateException {
			return;
		}
	}
	
	/**
	 * 忽略HTTPS请求的SSL证书
	 * @throws Exception
	 */
	public static void ignoreSSLVerify(){
		try {
			HttpsURLConnection.setDefaultSSLSocketFactory(getSslContext().getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(getHostnameVerifier());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
