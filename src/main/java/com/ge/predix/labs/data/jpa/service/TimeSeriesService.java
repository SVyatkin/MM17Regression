package com.ge.predix.labs.data.jpa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.ge.predix.labs.rest.RestClient;
import com.ge.predix.labs.rest.RestConfig;

@ComponentScan("com.ge.predix.labs")
@Service
public class TimeSeriesService {

	@Autowired
	protected RestConfig restConfig;
	@Autowired
	protected RestClient restClient;
	
	private static final Logger log = LoggerFactory.getLogger(TimeSeriesService.class);

	public ResponseEntity<String> postTS(String body) {
		return restClient.call(HttpMethod.POST, restConfig.httpsUrlTS, body);
	}
	
	public ResponseEntity<String> postTS(String url, String body) {
		return restClient.call(HttpMethod.POST, url, body);
	}

	public String getAssetById(String id) {
		log.info("no asset in the cache: " + id);
		return restClient.get(restConfig.httpsUrlA + id);
	}

	public String getGel(String domain, String filter) {
		String gel = domain + "?filter=" + filter;
		log.info("call gel: " + gel);
		return restClient.get(restConfig.httpsUrlA + "/" +gel);
	}

	public String getAssets(String domain) {
		return restClient.get(restConfig.httpsUrlA + "/" + domain);
	}
}
