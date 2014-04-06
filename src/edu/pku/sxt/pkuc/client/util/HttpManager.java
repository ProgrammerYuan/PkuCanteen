package edu.pku.sxt.pkuc.client.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.util.Log;

/**
 * HTTP Communication Utility
 * @author songxintong
 * 
 */
public class HttpManager {

	/****** SETTINGS ******/
	
	// timeout parameters, in milliseconds
	private static final int CONNECTION_TIMEOUT = 5000;
	private static final int SOCKET_TIMEOUT = 5000;
	private static final int READ_TIMEOUT = 5000;
	
	/****** END OF SETTINGS ******/
	
	private static final String LOG_TAG = "HttpManager";
	
	// DO NOT TOUCH!
	// these constants are used in the method:
	// 		postFile(String, Map<String, String>, Map<String, File>)
	// do not touch them unless you are rewriting or removing this method
	private static final String BOUNDARY = java.util.UUID.randomUUID().toString();
	private static final String PREFIX = "--";
	private static final String LINEND = "\r\n";
	private static final String MULTIPART_FROM_DATA = "multipart/form-data"; 
	private static final String CHARSET = "UTF-8";
	
	/**
	 * Make a HTTP POST conversation that both request and response
	 * contains key-value pairs only.
	 * @param url Server URL.
	 * @param params Key-value pairs in the request.
	 * @return Key-value pairs in the response.
	 * @throws Exception
	 */
	public static Map<String, String> postKV(String url,
			Map<String, String> params) throws HttpManagerException {
	
		// convert parameters
		List<NameValuePair> reqParams = new ArrayList<NameValuePair>();
		Set<String> key = params.keySet();
		Iterator<String> it = key.iterator();
		while (it.hasNext()) {
			String k = it.next();
			String v = params.get(k);
			reqParams.add(new BasicNameValuePair(k, v));
		}
		
		// set timeout
		HttpClient client = new DefaultHttpClient();
		HttpParams httpParams = client.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParams, SOCKET_TIMEOUT);
		
		// do post
		HttpPost request = new HttpPost(url);
		String result = "";
		try {
			request.setEntity(new UrlEncodedFormEntity(reqParams,HTTP.UTF_8));
			HttpResponse response = new DefaultHttpClient().execute(request);
			// check response
			if (response.getStatusLine().getStatusCode() == 200) {
				result = EntityUtils.toString(response.getEntity());
			} else {
				throw new HttpManagerException("Http Status "
						+ response.getStatusLine().getStatusCode());
			}
		} catch (Exception e) {
			throw new HttpManagerException(e.getMessage());
		}
		
		// convert result and return
		Map<String, String> ret = new HashMap<String, String>();
		Log.d(LOG_TAG, "http response received: " + result); // log response
		String[] pairs = result.split("&");
		for (int i = 0; i < pairs.length; i++) {
			String[] pair = pairs[i].split("=");
			String k = convertFromEscape(pair[0]);
			String v = convertFromEscape(pair[1]);
			ret.put(k, v);
		}
		return ret;
	}

	/**
	 * Make a HTTP POST conversation that the request contains
	 * both key-value pairs and files while the response contains
	 * key-value pairs only.
	 * @param urlstr Server URL.
	 * @param params Key-value pairs in the request.
	 * @param files Files in the request.
	 * @return Key-value pairs in the response.
	 * @throws Exception
	 */
	public static Map<String, String> postFile(String urlstr, Map<String, String> params,
			Map<String, File> files) throws HttpManagerException {
		
		String result = "";
		try {
			// set HTTP headers
			URL url = new URL(urlstr); 
			HttpURLConnection conn = (HttpURLConnection) url.openConnection(); 
			conn.setReadTimeout(READ_TIMEOUT); 
			conn.setDoInput(true);
			conn.setDoOutput(true); 
			conn.setUseCaches(false); 
			conn.setRequestMethod("POST"); 
			conn.setRequestProperty("connection", "keep-alive"); 
			conn.setRequestProperty("Charsert", "UTF-8"); 
			conn.setRequestProperty("Content-Type", MULTIPART_FROM_DATA + ";boundary=" + BOUNDARY); 
			
			// send key-value pair parameters
			StringBuilder sb = new StringBuilder(); 
			for (Map.Entry<String, String> entry : params.entrySet()) { 
				sb.append(PREFIX); 
				sb.append(BOUNDARY); 
				sb.append(LINEND); 
				sb.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + LINEND);
				sb.append("Content-Type: text/plain; charset=" + CHARSET+LINEND);
				sb.append("Content-Transfer-Encoding: 8bit" + LINEND);
				sb.append(LINEND);
				sb.append(entry.getValue()); 
				sb.append(LINEND); 
			} 
			DataOutputStream outStream = new DataOutputStream(conn.getOutputStream()); 
			outStream.write(sb.toString().getBytes()); 
			
			// send files 
			if(files != null)
			for (Map.Entry<String, File> file: files.entrySet()) { 
				StringBuilder sb1 = new StringBuilder(); 
				sb1.append(PREFIX); 
				sb1.append(BOUNDARY); 
				sb1.append(LINEND); 
				sb1.append("Content-Disposition: form-data; name=\"" + file.getKey() + "\"; filename=\""+file.getKey()+"\""+LINEND);
				sb1.append("Content-Type: application/octet-stream; charset="+CHARSET+LINEND);
				sb1.append(LINEND);
				outStream.write(sb1.toString().getBytes()); 
		
				InputStream is = new FileInputStream(file.getValue());
				byte[] buffer = new byte[1024]; 
				int len = 0; 
				while ((len = is.read(buffer)) != -1) { 
					outStream.write(buffer, 0, len); 
				} 
				is.close(); 
				outStream.write(LINEND.getBytes()); 
			} 
	
			// finish request
			byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes(); 
			outStream.write(end_data); 
			outStream.flush(); 
			
			// get response
			int res = conn.getResponseCode(); 
			if (res == 200) {
				InputStream in = conn.getInputStream(); 
				int ch; 
				StringBuilder sb2 = new StringBuilder(); 
				while ((ch = in.read()) != -1) { 
					sb2.append((char) ch); 
				} 
				outStream.close(); 
				conn.disconnect();
				result = sb2.toString();
			}  else {
				throw new Exception("Http Status " + res);
			}
		} catch (Exception e) {
			throw new HttpManagerException(e.getMessage());
		}
		
		// convert result
		Map<String, String> ret = new HashMap<String, String>();
		Log.d(LOG_TAG, "http response received: " + result); // log response
		String[] pairs = result.split("&");
		for (int i = 0; i < pairs.length; i++) {
			String[] pair = pairs[i].split("=");
			String k = convertFromEscape(pair[0]);
			String v = convertFromEscape(pair[1]);
			ret.put(k, v);
		}
		return ret;
	}
	
	/**
	 * Make a HTTP POST conversation that the request contains
	 * key-value pairs only while the response contains one
	 * file only.
	 * @param url Server URL.
	 * @param params Key-value pairs in the request.
	 * @return The file in the response as byte[].
	 * @throws Exception
	 */
	public static byte[] postForFile(String url,
			Map<String, String> params) throws HttpManagerException {

		try {
			// convert parameters
			List<NameValuePair> reqParams = new ArrayList<NameValuePair>();
			Set<String> key = params.keySet();
			Iterator<String> it = key.iterator();
			while (it.hasNext()) {
				String k = it.next();
				String v = params.get(k);
				reqParams.add(new BasicNameValuePair(k, v));
			}
			
			// set timeout
			HttpClient client = new DefaultHttpClient();
			HttpParams httpParams = client.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(httpParams, SOCKET_TIMEOUT);
			
			// do post
			HttpPost request = new HttpPost(url);
			request.setEntity(new UrlEncodedFormEntity(reqParams,HTTP.UTF_8));
			HttpResponse response = new DefaultHttpClient().execute(request);
	
			// check response
			if (response.getStatusLine().getStatusCode() == 200) {
				return EntityUtils.toByteArray(response.getEntity());
			} else {
				throw new Exception("Http Status "
						+ response.getStatusLine().getStatusCode());
			}
		} catch (Exception e) {
			throw new HttpManagerException(e.getMessage());
		}
		
	}
	
	/**
	 * Convert from escape String
	 * Rules:
	 * 	'#0' -> '#' 
	 * 	'#1' -> '=' 
	 * 	'#2' -> '&'
	 */
	private static String convertFromEscape(String s) {
		s = s.replace("#1", "=");
		s = s.replace("#2", "&");
		s = s.replace("#0", "#");
		return s;	
	}
}
