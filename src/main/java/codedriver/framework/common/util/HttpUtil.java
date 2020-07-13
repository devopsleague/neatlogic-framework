package codedriver.framework.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在java中处理http请求.
 * 
 */
public class HttpUtil {
	protected static Logger logger = LoggerFactory.getLogger(HttpUtil.class);
	static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			return;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			return;
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	} };

	public static String getHttpConnectionData(String url, String authMode, String accessKey, String accessSecret, String restfulType, String encodingType) {
		HttpURLConnection connection = null;
		InputStream is = null;
		// OutputStream os = null;
		BufferedReader br = null;
		StringBuffer result = new StringBuffer();
		try {
			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String paramString, SSLSession paramSSLSession) {
					return true;
				}
			});
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			URL getUrl = new URL(url);
			connection = (HttpURLConnection) getUrl.openConnection();
			String authorization = "";
			String currentTime = Long.toString(System.currentTimeMillis());
			if (StringUtils.isNotBlank(authMode)) {
				if (authMode.equalsIgnoreCase("basic")) {
					String sign = accessKey + ":" + accessSecret;
					authorization = "Basic " + Base64.encodeBase64String(sign.getBytes("utf-8"));
				}
				// TODO linbq这是临时给前端测试用的，后面要删
				if (authMode.equalsIgnoreCase("basic")) {
					connection.setRequestProperty("Tenant", "techsure");
					authorization = "Bearer_eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJyb2xlbGlzdCI6WyJBRE1JTiJdLCJ1c2VyaWQiOiJsdnprIiwidGVuYW50IjoidGVjaHN1cmUiLCJ1c2VybmFtZSI6IuWQleS9kOW6tyJ9.nA3LWLVkQ4WM57XaMJRIbYllAY8=";
				}
			}
			connection.setUseCaches(false);
			connection.setRequestMethod(restfulType.toUpperCase());
			if ("post".equalsIgnoreCase(restfulType)) {
				connection.setDoOutput(true);
				connection.setDoInput(true);
			}
			connection.setRequestProperty("x-access-date", currentTime);
			connection.setRequestProperty("Authorization", authorization);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept-Charset", "UTF-8");
			connection.setRequestProperty("Accept", "text/plain, application/json, */*");
			connection.connect();

			if (connection != null) {
				try {
					int code = connection.getResponseCode();

					if (String.valueOf(code).startsWith("2")) {
						InputStream in = connection.getInputStream();
						InputStreamReader isr = new InputStreamReader(in, encodingType);
						BufferedReader reader = new BufferedReader(isr);
						logger.info("http request succeed.status code is " + code);
						String lines;
						while ((lines = reader.readLine()) != null) {
							result.append(lines);
						}
					} else if (String.valueOf(code).startsWith("5")) {
						InputStream in = connection.getErrorStream();
						InputStreamReader isr = new InputStreamReader(in, encodingType);
						BufferedReader reader = new BufferedReader(isr);
						logger.info("http request succeed.status code is " + code);
						String lines;
						while ((lines = reader.readLine()) != null) {
							result.append(lines);
						}
					}
					logger.info(url + " rest return status:" + code + ",url is " + url + ",data is:" + result.toString());
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return result.toString();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			connection.disconnect();
		}

		return result.toString();
	}
}