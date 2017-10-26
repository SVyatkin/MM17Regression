package com.ge.predix.labs.data.jpa.web;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ge.predix.labs.data.jpa.dto.Attribute;
import com.ge.predix.labs.data.jpa.dto.Prediction;
import com.ge.predix.labs.data.jpa.service.TimeSeriesService;
import com.ge.predix.labs.rest.RestConfig;

@ComponentScan
@RestController
@CrossOrigin()
public class TimeSeriesController {

	@Autowired
	private TimeSeriesService timeSeriesService;

	@Autowired
	private RestConfig restConfig;

	private static final Logger log = LoggerFactory.getLogger(TimeSeriesController.class);

	public static final String TIME_SERIES = "/ts";
	public static final String TIME_SERIES_DATAPOINTS = "/ts/data/{name}";
	public static final String TIME_SERIES_TEMP_DATAPOINTS = "/ts/temp/{name}";
	public static final String TIME_SERIES_PREDICT = "/ts/predict/{name}";

	private static Map<String, List<Double>> regionsMap = new ConcurrentHashMap<>();
	private static Map<String, List<Double>> regionsTempMap = new ConcurrentHashMap<>();
	private static Map<String, List<Double>> regionsMapDays = new ConcurrentHashMap<>();
	private static Map<String, List<Double>> regionsTempMapDays = new ConcurrentHashMap<>();

	/**
	 * POST to get prediction
	 */
	@RequestMapping(value = TIME_SERIES_PREDICT, method = RequestMethod.POST)
	public List<Point> getForecastPost(@PathVariable String name, @RequestBody Prediction prediction)
			throws JsonParseException, JsonMappingException, IOException {
		if ("hr".equalsIgnoreCase(prediction.getUnit()))
			return getHRPrediction(name, prediction);
		else if ("day".equalsIgnoreCase(prediction.getUnit()))
			return getDayPrediction(name, prediction);
		return null;
	}

	private List<Point> getDayPrediction(String name, Prediction prediction) {
		List<Double> power = regionsMapDays.get(name);
		List<Double> temperature = regionsTempMapDays.get(name);

		List<Point> list = new LinkedList<>();
		// hours prediction

		// get start point from prediction dto
		long startTimeStamp = getOsTime(prediction.getStartDate()).getTime();
		int start = getStartDay(prediction.getStartDate()) - 20;
		System.out.println("start date: " + start);
		for (int i = 0; i < prediction.getTimes(); i++) {
			SimpleRegression simpleRegression = getRegression(power, temperature, start + i, 1);
			long tm = startTimeStamp + i* 24 * 60L * 60L * 1000L;
			Point point = new Point(tm, simpleRegression.predict(prediction.getTemperatures()[i]));
			System.out.println("prediction for ["+ prediction.getTemperatures()[i] +"] = "  + point.getValue());
			list.add(point);

			System.out.println("slope = " + simpleRegression.getSlope());
			System.out.println("intercept = " + simpleRegression.getIntercept());
			System.out.println("sum@2 = " + simpleRegression.getTotalSumSquares());
			System.out.println("Intercept@2 = " + simpleRegression.getIntercept());
			System.out.println("sum@2 Err= " + simpleRegression.getMeanSquareError());
		}
		return list;
	}

	private List<Point> getHRPrediction(String name, Prediction prediction) {
		// get prepared dataset
		List<Double> power = regionsMap.get(name);
		List<Double> temperature = regionsTempMap.get(name);

		List<Point> list = new LinkedList<>();
		// hours prediction

		// get start point from prediction dto
		long startTimeStamp = getOsTime(prediction.getStartDate()).getTime();
		int start = getStartPoint(prediction.getStartDate());
		System.out.println("start date: " + start);
		for (int i = 0; i < prediction.getTimes(); i++) {
			SimpleRegression simpleRegression = getRegression(power, temperature, start + i, 24);
			long tm = startTimeStamp + i * 60L * 60L * 1000L;
			Point point = new Point(tm, simpleRegression.predict(prediction.getTemperatures()[i]));
			System.out.println("prediction for ["+ prediction.getTemperatures()[i] +"] = "  + point.getValue());
			list.add(point);

			System.out.println("slope = " + simpleRegression.getSlope());
			System.out.println("intercept = " + simpleRegression.getIntercept());
			System.out.println("sum@2 = " + simpleRegression.getTotalSumSquares());
			System.out.println("Intercept@2 = " + simpleRegression.getIntercept());
			System.out.println("sum@2 Err= " + simpleRegression.getMeanSquareError());
		}
		return list;
	}

	private int getStartPoint(String startDate) {
		Date date = getOsTime(startDate);
		int st = (int) ((date.getTime() - 1463468400000L) / (24 * 60 * 60 * 1000));
		int stDay = (int) ((date.getTime() - 1463468400000L) / (24 * 60 * 60 * 1000) % 364);
		int startPoint = (int) (stDay * 24 + date.getHours());
		return startPoint;
	}

	private int getStartDay(String startDate) {
		Date date = getOsTime(startDate);
		int st = (int) ((date.getTime() - 1463468400000L) / (24 * 60 * 60 * 1000));
		int stDay = (int) ((date.getTime() - 1463468400000L) / (24 * 60 * 60 * 1000) % 364);
		return stDay;
	}

	private Date getOsTime(String startDate) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

		Date date = null;
		try {

			date = formatter.parse(startDate);
			System.out.println(date.getTime());

		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	private SimpleRegression getRegression(List<Double> power, List<Double> temperature, int start, int interval) {
		int k = 0;
		double[][] arr = new double[20][20];
		SimpleRegression simpleRegression = new SimpleRegression(true);
		for (int i = 0; i < 20; i++) {
			arr[i][0] = temperature.get(start + i * interval);
			arr[i][1] = power.get(start + i * interval);
			System.out.println(arr[i][0] + ":" + arr[i][1]);
		}

		simpleRegression.addData(arr);
		return simpleRegression;
	}

	/**
	 * GET
	 */

	@RequestMapping(value = TIME_SERIES_DATAPOINTS, method = RequestMethod.GET)
	public List<Double> getRegDatapoints(@PathVariable String name)
			throws JsonParseException, JsonMappingException, IOException {
		restConfig.checkTokenExpires();
		String body = "{\"start\": 1463468400000,\"end\": \"1s-ago\",\"tags\": [{\"name\": \"GC13\"}]}";
		String body2 = "{\"start\": 1463468400000,\"end\": \"1s-ago\",\"tags\": [{\"name\": \"CC18\"}]}";
		String body3 = "{\"start\": 1463468400000,\"end\": \"1s-ago\",\"tags\": [{\"name\": \"CC15\"}]}";
		log.info(body);
		String reBody = getTSResponse(body);
		List<Double> datapointsArr = new ArrayList<Double>();
		getTimeSeriesDatapointsD(reBody, datapointsArr);
		System.out.println("Feeder1:" + datapointsArr.size());
		reBody = getTSResponse(body2);
		getTimeSeriesDatapointsAdd(reBody, datapointsArr);
		reBody = getTSResponse(body3);
		getTimeSeriesDatapointsAdd(reBody, datapointsArr);
		regionsMap.put(name, datapointsArr);
		regionsMapDays.put(name, getRegionDataByDay(datapointsArr));
		System.out.println("Feeders:" + datapointsArr.size());
		return datapointsArr;
	}

	private List<Double> getRegionDataByDay(List<Double> datapointsArr) {
		List<Double> days = new ArrayList<Double>();
		Double day = 0.0;
		int count = 0;
		for (Double point : datapointsArr) {
			day += point;
			count++;
			if (count % 24 == 0) {
				days.add(day);
				count = 0;
				day = 0.0;
				;
			}
		}
		return days;
	}

	private List<Double> getRegionAverageDataByDay(List<Double> datapointsArr) {
		List<Double> days = new ArrayList<Double>();
		Double day = 0.0;
		int count = 0;
		for (Double point : datapointsArr) {
			day += point;
			count++;
			if (count % 24 == 0) {
				days.add(day / 24);
				count = 0;
				day = 0.0;
				;
			}
		}
		return days;
	}

	@RequestMapping(value = TIME_SERIES_TEMP_DATAPOINTS, method = RequestMethod.GET)
	public List<Double> getRegDatapointsTemp(@PathVariable String name)
			throws JsonParseException, JsonMappingException, IOException {
		restConfig.checkTokenExpires();
		String body = "{\n" + "    \"start\": 1463468400000,\n" + "    \"tags\": [\n" + "        {\n"
				+ "            \"name\": \"temp\",\n" + "            \"filters\": {\n"
				+ "                \"attributes\": {\n" + "                       \"location\": [\n"
				+ "                                  \"Letterkenny_Dromore\"\n" + "                        ]\n"
				+ "                }\n" + "            }\n" + "        }\n" + "    ]\n" + " }";
		log.info(body);
		String reBody = getTSResponse(body);
		List<Double> datapointsArr = new ArrayList<Double>();
		getTimeSeriesDatapointsForTemp(reBody, datapointsArr);
		regionsTempMap.put(name, datapointsArr);
		regionsTempMapDays.put(name, getRegionAverageDataByDay(datapointsArr));
		System.out.println("Temp:" + datapointsArr.size());
		return datapointsArr;
	}

	private String getTSResponse(String body) {
		ResponseEntity<String> responseEntity = timeSeriesService.postTS(body);
		String reBody = "";
		if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
			// get data points from Time Series
			reBody = responseEntity.getBody();
		}
		return reBody;
	}

	private List<Double> getTimeSeriesDatapointsD(String reBody, List<Double> datapointsArr)
			throws IOException, JsonParseException, JsonMappingException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode nodes = objectMapper.readValue(reBody, JsonNode.class);
		JsonNode tags = nodes.get("tags");
		if (tags.isArray()) {
			// TAG NAME
			for (final JsonNode tag : tags) {
				String tagName = tag.get("name").asText();
				System.out.println("name" + ":" + tagName);
				JsonNode results = tag.get("results");
				JsonNode rs = results.get(0);
				// ATTRIBUTES
				JsonNode attributes = rs.get("attributes");
				Iterator<Entry<String, JsonNode>> nodes1 = attributes.getFields();
				List<Attribute> attrList = new ArrayList<>();
				while (nodes1.hasNext()) {
					Attribute attr = new Attribute();
					Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodes1.next();
					JsonNode entr = entry.getValue();
					attr.setName(entry.getKey());
					if (entr.isArray()) {
						for (final JsonNode e : entr) {
							attr.setValue(e.getTextValue());
						}
					}
					attrList.add(attr);
				}
				// DATA POINTS
				JsonNode datapoints = rs.get("values");
				if (datapoints.isArray()) {
					int i = 0;
					Double temp = 0.0;
					for (final JsonNode datapoint : datapoints) {
						i++;
						if (i % 2 == 0) {
							datapointsArr.add(datapoint.get(1).asDouble() + temp);
						} else {
							temp = datapoint.get(1).asDouble();
						}

					}
				}
			}
		}
		return datapointsArr;
	}

	private List<Double> getTimeSeriesDatapointsForTemp(String reBody, List<Double> datapointsArr)
			throws IOException, JsonParseException, JsonMappingException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode nodes = objectMapper.readValue(reBody, JsonNode.class);
		JsonNode tags = nodes.get("tags");
		if (tags.isArray()) {
			// TAG NAME
			for (final JsonNode tag : tags) {
				String tagName = tag.get("name").asText();
				System.out.println("name" + ":" + tagName);
				JsonNode results = tag.get("results");
				JsonNode rs = results.get(0);
				// ATTRIBUTES
				JsonNode attributes = rs.get("attributes");
				Iterator<Entry<String, JsonNode>> nodes1 = attributes.getFields();
				List<Attribute> attrList = new ArrayList<>();
				while (nodes1.hasNext()) {
					Attribute attr = new Attribute();
					Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodes1.next();
					JsonNode entr = entry.getValue();
					attr.setName(entry.getKey());
					if (entr.isArray()) {
						for (final JsonNode e : entr) {
							attr.setValue(e.getTextValue());
						}
					}
					attrList.add(attr);
				}
				// DATA POINTS
				JsonNode datapoints = rs.get("values");
				if (datapoints.isArray()) {
					for (final JsonNode datapoint : datapoints) {
						datapointsArr.add(datapoint.get(1).asDouble());
					}
				}
			}
		}
		return datapointsArr;
	}

	private List<Double> getTimeSeriesDatapointsAdd(String reBody, List<Double> datapointsArr)
			throws IOException, JsonParseException, JsonMappingException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode nodes = objectMapper.readValue(reBody, JsonNode.class);
		JsonNode tags = nodes.get("tags");
		if (tags.isArray()) {
			// TAG NAME
			for (final JsonNode tag : tags) {
				String tagName = tag.get("name").asText();
				System.out.println("name" + ":" + tagName);
				JsonNode results = tag.get("results");
				JsonNode rs = results.get(0);
				// ATTRIBUTES
				JsonNode attributes = rs.get("attributes");
				Iterator<Entry<String, JsonNode>> nodes1 = attributes.getFields();
				List<Attribute> attrList = new ArrayList<>();
				while (nodes1.hasNext()) {
					Attribute attr = new Attribute();
					Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodes1.next();
					JsonNode entr = entry.getValue();
					attr.setName(entry.getKey());
					if (entr.isArray()) {
						for (final JsonNode e : entr) {
							attr.setValue(e.getTextValue());
						}
					}
					attrList.add(attr);
				}
				// DATA POINTS
				JsonNode datapoints = rs.get("values");
				if (datapoints.isArray()) {
					int i = 0;
					int j = 0;
					Double temp = 0.0;
					for (final JsonNode datapoint : datapoints) {
						i++;
						if (i % 2 == 0) {
							datapointsArr.set(j, datapoint.get(1).asDouble() + temp + datapointsArr.get(j));
							j++;
						} else {
							temp = datapoint.get(1).asDouble();
						}

					}
				}
			}
		}
		return datapointsArr;
	}

}