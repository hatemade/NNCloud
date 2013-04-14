package net.kiknlab.nncloud.sensor;

import net.kiknlab.nncloud.db.LocationLogDBManager;
import net.kiknlab.nncloud.util.LocationLog;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class GPSSensor implements LocationListener{
	private Context mContext;
	private LocationManager locationManager;
	//milli seconds
	private static final long MIN_DURATION = 0;
	
	public GPSSensor(Context context) {
		this.mContext = context;
	}
	
	public void start() {
		 locationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
		 
		 //TODO GPS or NetworkÇîªífÇ∑ÇÈ
		 // à íuèÓïÒÇÃçXêVÇéÛÇØéÊÇÈÇÊÇ§Ç…ê›íË
		 locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_DURATION, 0, this);
	}
	
	public void stop() {
		//TODO stop logging
		locationManager.removeUpdates(this);
	}

	@Override
	public void onLocationChanged(Location arg0) {
		Log.e("d", arg0.getLatitude() + "," + arg0.getLongitude());
		LocationLog log = new LocationLog(arg0.getLatitude(), arg0.getLongitude(),
				java.lang.System.currentTimeMillis());
		LocationLogDBManager.insertSensorData(mContext, log);
		Log.e("debug", "Location Changed");
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
}
