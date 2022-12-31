package https.service;

import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import utils.FileUtilities;

public class HttpsClientService {

	private static HttpsURLConnection httpsUrlConnection = null;

	private static Logger log = Logger.getLogger(HttpsClientService.class);

	public static HttpsURLConnection getHttpsURLConnection(String urlString) throws Exception {

		try {
			URL url = new URL(urlString);
			URLConnection urlConnection = url.openConnection();
			httpsUrlConnection = (HttpsURLConnection) urlConnection;
			SSLSocketFactory sslSocketFactory = createSslSocketFactory();
			httpsUrlConnection.setSSLSocketFactory(sslSocketFactory);
		} catch (Exception e) {
			throw e;
		}

		// log.info("Returning HttpsURLConnection object for " + urlString);
		return httpsUrlConnection;
	}

	private static SSLSocketFactory createSslSocketFactory() throws Exception {
		TrustManager[] byPassTrustManagers = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			public void checkClientTrusted(X509Certificate[] chain, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] chain, String authType) {
			}
		} };
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, byPassTrustManagers, new SecureRandom());
		return sslContext.getSocketFactory();
	}

	public static void closeHttpsURLConnection() throws Exception {
		try {
			if (httpsUrlConnection != null) {
				httpsUrlConnection.disconnect();
				// log.info("HttpsURLConnection object has been disconnected");
			}
		} catch (Exception e) {
			throw e;
		}
	}

	public static String jsoupExtraction(String url) throws Exception {
		try {
			url = url.contains("http://") ? url.replace("http://", "https://") : url;
			String html = FileUtilities.streamHttpsUrlConnection(getHttpsURLConnection(url), false);
			return html;

		} catch (Exception e) {
			throw e;
		}
	}

}
