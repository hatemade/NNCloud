package net.kiknlab.nncloud.util;

public class LocationLog {
	
	private double latitude;
	private double longitude;
	private long timestamp;
	
	public LocationLog(double latitude, double longitude, long timestamp) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.timestamp = timestamp;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	
}
