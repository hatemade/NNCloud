package net.kiknlab.nncloud.db;

import java.util.ArrayList;

import net.kiknlab.nncloud.util.StateLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DayLogDBManager {
	static final String TABLE_NAME = "day_logs";
	static final String COL_ID = "id";
	static final String COL_STEP = "step";
	static final String COL_TIMESTAMP = "timestamp";
	public static final String CREATE_TABLE_SQL = "create table " + 
			TABLE_NAME + "(" +
			COL_ID + " integer primary key autoincrement," +
			COL_STEP + " integer not null," +
			COL_TIMESTAMP + " text not null" +
			")";
	public static final String DROP_TABLE_SQL = "drop table if exists " + TABLE_NAME;

	public static boolean insertStepLog(Context context, int step, long timestamp){
		ContentValues values = new ContentValues();
		values.put(COL_STEP, step);
		values.put(COL_TIMESTAMP, timestamp);

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

	/*
	public static ArrayList<StateLog> getStepLogList(Context context) {
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null, null);

		ArrayList<StateLog> logs = new ArrayList<StateLog>();
		try {
			while (cursor.moveToNext()) {
				logs.add(new StateLog(
						cursor.getInt(1),
						cursor.getLong(2)));
			}
			return logs;
		} catch (Exception e) {
			e.printStackTrace();
			return logs;
		}
	}
	*/
	public static int getStepLogListOnDay(Context context, long time) {
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(
				TABLE_NAME,
				new String[]{"sum(" + COL_STEP + ")"},
				COL_TIMESTAMP + " between ? and ?",
				new String[]{time + "", (time + (1000 * 60 * 60 * 24)) + ""},
				null, null,
				COL_ID + " DESC",
				null);

		int step = 0;
		try {
			if(cursor.moveToFirst()){
				step = cursor.getInt(0);
			}
			return step;
		} catch (Exception e) {
			e.printStackTrace();
			return step;
		}
	}
	public static int countDatas(Context context){
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, new String[]{"count(*)"},null, null, null, null, null);
		StringBuilder str = new StringBuilder();

		try {
			while (cursor.moveToNext()) {
				for(int i = 0;i < cursor.getColumnCount();i++){
					str.append(cursor.getString(i));
					str.append(",");
				}
				str.append("\n");
			}
			Log.e("CountData", str.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}
