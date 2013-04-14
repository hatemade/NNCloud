package net.kiknlab.nncloud.db;

import java.util.ArrayList;
import java.util.Date;

import net.kiknlab.nncloud.util.LocationLog;
import net.kiknlab.nncloud.util.SensorData;
import net.kiknlab.nncloud.util.StateLog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class LocationLogDBManager {
	static final String TABLE_NAME = "location_logs";
	static final String COL_ID = "id";
	static final String COL_LATITUDE = "latitude";
	static final String COL_LONGITUDE = "longitude";
	static final String COL_TIMESTAMP = "timestamp";
	public static final String CREATE_TABLE_SQL = "create table " + 
			TABLE_NAME + "(" +
			COL_ID + " integer primary key autoincrement," +
			COL_LATITUDE + " real not null," +
			COL_LONGITUDE + " real not null," + 
			COL_TIMESTAMP + " text not null" +
			")";
	public static final String DROP_TABLE_SQL = "drop table if exists " + TABLE_NAME;

	public static boolean insertSensorData(Context context, LocationLog locationLog){
		ContentValues values = new ContentValues();
		values.put(COL_LATITUDE, locationLog.getLatitude());
		values.put(COL_LONGITUDE,  locationLog.getLongitude());
		values.put(COL_TIMESTAMP, locationLog.getTimestamp());
		
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getWritableDatabase();
		try{
			db.insert(TABLE_NAME, null, values);
			values.clear();
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static ArrayList<LocationLog> getLocationLogList(Context context) {
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null, null);

		ArrayList<LocationLog> logs = new ArrayList<LocationLog>();
		try {
			while (cursor.moveToNext()) {
				logs.add(new LocationLog(
						cursor.getDouble(1),
						cursor.getDouble(2),
						cursor.getLong(3)));
			}
			return logs;
		} catch (Exception e) {
			e.printStackTrace();
			return logs;
		}
	}
	public static ArrayList<LocationLog> getLocationLogListOnDay(Context context, long time) {
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(
				TABLE_NAME,
				null,
				COL_TIMESTAMP + " between ? and ?",
				new String[]{time + "", (time + (1000 * 60 * 60 * 24)) + ""},
				null, null,
				COL_ID + " DESC",
				null);

		ArrayList<LocationLog> logs = new ArrayList<LocationLog>();
		try {
			while (cursor.moveToNext()) {
				logs.add(new LocationLog(
						cursor.getDouble(1),
						cursor.getDouble(2),
						cursor.getLong(3)));
			}
			return logs;
		} catch (Exception e) {
			e.printStackTrace();
			return logs;
		}
	}
}
