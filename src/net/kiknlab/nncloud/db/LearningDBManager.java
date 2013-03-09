package net.kiknlab.nncloud.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class LearningDBManager {
	LearningDBHelper transactionHelper;
	SQLiteDatabase transactionDb;
	ContentValues transactionValues;

	public void beginTransaction(Context context){
		transactionHelper = LearningDBHelper.getInstance(context);
		transactionDb = transactionHelper.getWritableDatabase();
		transactionDb.beginTransaction();
	}

	public void endTransaction(){
		transactionDb.endTransaction();
	}

	public void insertTransaction(Context context, float data, int type, int accuracy, long timestamp){
		transactionValues = new ContentValues();
		transactionValues.put(LearningDBHelper.COL_DATA1, data);
		transactionValues.put(LearningDBHelper.COL_TYPE, type);
		transactionValues.put(LearningDBHelper.COL_ACCURACY, accuracy);
		transactionValues.put(LearningDBHelper.COL_TIMESTAMP, timestamp);
		insertValuesTransaction(context, transactionValues);
	}

	public void insertTransaction(Context context, float[] data, int type, int accuracy, long timestamp){
		transactionValues = new ContentValues();
		transactionValues.put(LearningDBHelper.COL_DATA1, data[0]);
		transactionValues.put(LearningDBHelper.COL_DATA2, data[1]);
		transactionValues.put(LearningDBHelper.COL_DATA3, data[2]);
		transactionValues.put(LearningDBHelper.COL_TYPE, type);
		transactionValues.put(LearningDBHelper.COL_ACCURACY, accuracy);
		transactionValues.put(LearningDBHelper.COL_TIMESTAMP, timestamp);
		insertValuesTransaction(context, transactionValues);
	}
	
	public void insertValuesTransaction(Context context, ContentValues values){
		try{
			transactionDb.insert(LearningDBHelper.TABLE_NAME, null, values);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void getAllSensorData(Context context){
		LearningDBHelper helper = LearningDBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(LearningDBHelper.TABLE_NAME, null,null,null,null,null,null,"1");
		StringBuilder str = new StringBuilder();
		try {
			while (cursor.moveToNext()) {
				for(int i = 0;i < cursor.getColumnCount();i++){
					str.append(cursor.getString(i));
					str.append(",");
					//Log.e("database", "[" + cursor.getColumnName(i) + ":" + cursor.getString(i) + "]");
				}
				str.append("\n");
			}
			Log.e("database",str.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void insertSensorData(Context context, float data, int type, int accuracy, long timestamp){
		ContentValues values = new ContentValues();
		values.put(LearningDBHelper.COL_DATA1, data);
		values.put(LearningDBHelper.COL_TYPE, type);
		values.put(LearningDBHelper.COL_ACCURACY, accuracy);
		values.put(LearningDBHelper.COL_TIMESTAMP, timestamp);
		insertContentValues(context, values);
	}
	public static void insertSensorData(Context context, float[] data, int type, int accuracy, long timestamp){
		ContentValues values = new ContentValues();
		values.put(LearningDBHelper.COL_DATA1, data[0]);
		values.put(LearningDBHelper.COL_DATA2, data[1]);
		values.put(LearningDBHelper.COL_DATA3, data[2]);
		values.put(LearningDBHelper.COL_TYPE, type);
		values.put(LearningDBHelper.COL_ACCURACY, accuracy);
		values.put(LearningDBHelper.COL_TIMESTAMP, timestamp);
		insertContentValues(context, values);
	}

	public static void insertContentValues(Context context, ContentValues values){
		LearningDBHelper helper = LearningDBHelper.getInstance(context);
		SQLiteDatabase db = helper.getWritableDatabase();
		try{
			db.insert(LearningDBHelper.TABLE_NAME, null, values);
			values.clear();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static String getSensorData(Context context, int limit){
		LearningDBHelper helper = LearningDBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(LearningDBHelper.TABLE_NAME, null,null,null,null,null,LearningDBHelper.COL_ID + " DESC",limit+"");
		StringBuilder str = new StringBuilder();
		try {
			while (cursor.moveToNext()) {
				for(int i = 0;i < cursor.getColumnCount();i++){
					str.append(cursor.getString(i));
					str.append(",");
					//Log.e("database", "[" + cursor.getColumnName(i) + ":" + cursor.getString(i) + "]");
				}
				str.append("\n");
			}
			Log.e("database",str.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
