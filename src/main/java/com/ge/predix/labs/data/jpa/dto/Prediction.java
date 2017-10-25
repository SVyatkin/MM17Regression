package com.ge.predix.labs.data.jpa.dto;

public class Prediction {
	
	String unit;
	int times;
	String startDate;
	double[] temperatures;
	
	public String getUnit() {
		return unit;
	}
	public void setUnit(String unit) {
		this.unit = unit;
	}

	public int getTimes() {
		return times;
	}
	public void setTimes(int times) {
		this.times = times;
	}
	public String getStartDate() {
		return startDate;
	}
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	public double[] getTemperatures() {
		return temperatures;
	}
	public void setTemperatures(double[] temperatures) {
		this.temperatures = temperatures;
	}

}
