package com.ge.predix.labs.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class RestConfig {
    private static final Logger log = LoggerFactory.getLogger(RestConfig.class);

	public final HttpHeaders requestHeaders = new HttpHeaders();
	@Value("${asset.oauth.password}")
	public String passwordPair;
	@Value("${asset.zoneid}")
	public String timeZoneA;
	@Value("${asset.oauth.restHost}")
	public String predixOathHostname;
	@Value("${asset.service.base.url}")
	public String httpsUrlA;
	
	@Value("${ts.service.base.url}")
	public String httpsUrlTS;	
	@Value("${ts.zoneid}")
	public String timeZoneTS;
	
	private long tokenExpires;

	/**
	 * @return the tokenExpires
	 */
	
	public boolean isTokenExpires() {
		return tokenExpires < System.currentTimeMillis() + 100;
	}
	
	public void checkTokenExpires() {
		if (isTokenExpires()) {
			log.info(" **** Token expired - create new");
			initOauthToken();
		}
	}

	private void setTokenExpires(long asLong) {
        this.tokenExpires = System.currentTimeMillis() + asLong;
	}
	
	
	public String getTimeZone() {
		return timeZoneTS;
	}
	public String getHttpsUrl() {
		return httpsUrlTS;
	}


	@PostConstruct
	public void initHeader(){	
		initOauthToken();
	}
//		String predixOathHostname_ = System.getenv("ASSET_OAUTH_RESTHOST");
//		predixOathHostname = predixOathHostname_;
//		String passwordPair_ = System.getenv("ASSET_OAUTH_PASSWORD");
//		passwordPair = passwordPair_;
//		String timeZone_ = System.getenv("ASSET_ZONEID");
//		timeZoneA = timeZone_;
//		String httpsUrl_ = System.getenv("ASSET_SERVICE_BASE_URL");
//		httpsUrlA = httpsUrl_;
//		String timeZoneTS_ = System.getenv("TS_ZONEID");
//		timeZoneTS = timeZoneTS_;
//		String httpsUrlTS_ = System.getenv("TS_SERVICE_BASE_URL");
//		httpsUrlTS = httpsUrlTS_;
//	}


	public  void initOauthToken() {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		List<Header> headerList = new ArrayList<Header>();
		headerList.add(new BasicHeader("Content-Type","application/x-www-form-urlencoded"));
		String oauthHeader = "Basic " + new String(Base64.encodeBase64(passwordPair.getBytes()));
		headerList.add(new BasicHeader("Authorization", oauthHeader));
		headerList.add(new BasicHeader("Pragma", "no-cache"));
		String requestBody = "grant_type=client_credentials";
		String token = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode node = null;
			token = getToken(predixOathHostname, httpClient, requestBody, headerList);
			node = (ObjectNode) mapper.readTree(token);
			token = "Bearer " + node.get("access_token").asText();
			this.setTokenExpires(node.get("expires_in").asLong());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		initRequestHeaders (token);
	}

	public  List<Header> getOauthToken() {
		log.info("Get Token");
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		List<Header> headerList = new ArrayList<Header>();
		headerList.add(new BasicHeader("Content-Type","application/x-www-form-urlencoded"));
		String oauthHeader = "Basic " + new String(Base64.encodeBase64(passwordPair.getBytes()));
		headerList.add(new BasicHeader("Authorization", oauthHeader));
		headerList.add(new BasicHeader("Pragma", "no-cache"));
		String requestBody = "grant_type=client_credentials";
		String token = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode node = null;
			token = getToken(predixOathHostname, httpClient, requestBody, headerList);
			node = (ObjectNode) mapper.readTree(token);
			token = "Bearer " + node.get("access_token").asText();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info(token);
		initRequestHeaders (token);
		return addSecureTokenToHeaders(null,token);
	}
	
	
    private void initRequestHeaders(String token) {
    	requestHeaders.set("Content-Type", "application/json;charset=UTF-8");
    	requestHeaders.set("Authorization", token);
    	requestHeaders.set("Predix-Zone-Id", getTimeZone());
	}
    
	@SuppressWarnings("nls")
    public  List<Header> addSecureTokenToHeaders(List<Header> headers, String token)
    {
        List<Header> localHeaders = headers;
        if ( localHeaders == null ) localHeaders = new ArrayList<Header>();
        List<Header> headersToRemove = new ArrayList<Header>();
        for (Header header : localHeaders)
        {
            if ( header.getName().equals("Authorization") ) headersToRemove.add(header);
        }
        localHeaders.removeAll(headersToRemove);
        localHeaders.add(new BasicHeader("Authorization", token));
        localHeaders.add(new BasicHeader("Predix-Zone-Id", timeZoneA));
        return localHeaders;
    }

	private  String getToken(String url, CloseableHttpClient httpClient,
			String queryParams, List<Header> headers)
			throws UnsupportedEncodingException, IOException,
			ClientProtocolException {
		String url2 = url;
		url2 += "?" + queryParams;
		HttpGet method = new HttpGet(url2);
		method.setHeaders(headers.toArray(new Header[headers.size()]));
		CloseableHttpResponse httpResponse = httpClient.execute(method);
		if (httpResponse.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("unable able to connect to the UAA url="
					+ url2 + " response=" + httpResponse);
		}
		HttpEntity responseEntity = httpResponse.getEntity();
		httpResponse.close();
		String token = EntityUtils.toString(responseEntity);
		return token;
	}
}
