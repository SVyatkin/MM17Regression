package com.ge.predix.labs.data.jpa.web;

public class Point {
	
	long timestamp;
	double value;

	
	public Point(long tm, double predict) {
		this.timestamp = tm;
		this.value = predict;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		this.value = value;
	}
}
