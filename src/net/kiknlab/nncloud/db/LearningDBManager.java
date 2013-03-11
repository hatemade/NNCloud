package net.kiknlab.nncloud.db;

import java.util.ArrayList;

import net.kiknlab.nncloud.util.SensorData;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class LearningDBManager {
	LearningDBHelper transactionHelper;
	SQLiteDatabase transactionDb;
	ContentValues transactionValues;

	public void startTransaction(Context context){
		transactionHelper = LearningDBHelper.getInstance(context);
		transactionDb = transactionHelper.getWritableDatabase();
		transactionDb.beginTransaction();
	}

	public void beginTransaction(){
		transactionDb.beginTransaction();
	}

	public void endTransaction(){
		transactionDb.setTransactionSuccessful();
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
			values.clear();
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

	//センサのデータを指定したタイプと時間で取得
	public static ArrayList<SensorData> getSensorData(Context context, int type, long time) {
		LearningDBHelper helper = LearningDBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Log.e("今ここ","400:"+type);
		Cursor cursor = db.query(//指定された時間から最新のデータを集める
				LearningDBHelper.TABLE_NAME,
				null,
				"? = " + LearningDBHelper.COL_TYPE + " and ? >= " + LearningDBHelper.COL_TIMESTAMP,
				new String[]{type + "", time + ""},
				null, null,
				LearningDBHelper.COL_ID + " DESC",
				null);
		
		ArrayList<SensorData> datas = new ArrayList<SensorData>();
		try {
			while (cursor.moveToNext()) {
				Log.e("今ここ","A:"+cursor.getFloat(0)+":"+cursor.getFloat(1)+":"+cursor.getFloat(2)+":"+cursor.getFloat(3)+":"+cursor.getInt(4)+":"+cursor.getInt(5)+":"+cursor.getInt(6));
				datas.add(new SensorData(
						new float[]{cursor.getFloat(1), cursor.getFloat(2), cursor.getFloat(3)},
						type,//違うタイプが入っていたら問題なので、俺のqueryがtypeを間違えるわけがない
						cursor.getInt(5),
						cursor.getLong(6)));
			}
			return datas;
		} catch (Exception e) {
			e.printStackTrace();
			return datas;
		}
	}
		
	//とりあえずデータとって、ハッシュで種類に分けて格納して渡せばいいかなーと思ったけどセンサごとに個別に設定できるのはみりょくてきだったー
	//ってことでお蔵入り
	/*
	public static HashMap<String, ArrayList<SensorData>> getSensorData(Context context, long time) {
		LearningDBHelper helper = LearningDBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(//指定された時間から最新のデータを集める
				LearningDBHelper.TABLE_NAME,
				null,
				"? <= " + LearningDBHelper.COL_TIMESTAMP,
				new String[]{time + ""},
				null, null,
				LearningDBHelper.COL_TIMESTAMP + " ASC",
				null);
		
		//取得した値を種別ごとに入れる、入れたい
		HashMap<String, ArrayList<SensorData>> dataMap = new HashMap<String, ArrayList<SensorData>>();
		try {
			while (cursor.moveToNext()) {
				if()
				Long dateLong = cursor.getLong(0);
				String uriStr = cursor.getString(1);
				String thumbUriStr = cursor.getString(2);
				String typeStr = cursor.getString(3);
				if(thumbUriStr == null) {
					photos.add(new Photo(new Date(dateLong), Uri.parse(uriStr), typeStr));
				}
				else {
					photos.add(new Photo(new Date(dateLong), Uri.parse(uriStr), Uri.parse(thumbUriStr),typeStr));
				}
			}
			return photos;
		} catch (Exception e) {
			e.printStackTrace();
			return photos;
		}
	}
	*/
}
