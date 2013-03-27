package net.kiknlab.nncloud.db;

import java.io.FileOutputStream;
import java.util.ArrayList;

import net.kiknlab.nncloud.util.SensorData;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class LearningDBManager {
	static final String TABLE_NAME = "sensor_datas";
	static final String COL_ID = "id";
	static final String COL_DATA1 = "data1";
	static final String COL_DATA2 = "data2";
	static final String COL_DATA3 = "data3";
	static final String COL_TYPE = "type";
	static final String COL_ACCURACY = "accuracy";
	static final String COL_TIMESTAMP = "timestamp";
	public static final String CREATE_TABLE_SQL = "create table " + 
			TABLE_NAME + "(" +
			COL_ID + " integer primary key autoincrement," +
			COL_DATA1 + " real not null," +
			COL_DATA2 + " real," +
			COL_DATA3 + " real," +
			COL_TYPE + " integer not null," +
			COL_ACCURACY + " integer not null," +
			COL_TIMESTAMP + " text not null" +
			")";
	public static final String DROP_TABLE_SQL = "drop table if exists " + TABLE_NAME;
	DBHelper transactionHelper;
	SQLiteDatabase transactionDb;
	ContentValues transactionValues;


	public void startTransaction(Context context){
		transactionHelper = DBHelper.getInstance(context);
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
		transactionValues.put(COL_DATA1, data);
		transactionValues.put(COL_TYPE, type);
		transactionValues.put(COL_ACCURACY, accuracy);
		transactionValues.put(COL_TIMESTAMP, timestamp);
		insertValuesTransaction(context, transactionValues);
	}

	public void insertTransaction(Context context, float[] data, int type, int accuracy, long timestamp){
		transactionValues = new ContentValues();
		transactionValues.put(COL_DATA1, data[0]);
		transactionValues.put(COL_DATA2, data[1]);
		transactionValues.put(COL_DATA3, data[2]);
		transactionValues.put(COL_TYPE, type);
		transactionValues.put(COL_ACCURACY, accuracy);
		transactionValues.put(COL_TIMESTAMP, timestamp);
		insertValuesTransaction(context, transactionValues);
	}

	public void insertValuesTransaction(Context context, ContentValues values){
		try{
			transactionDb.insert(TABLE_NAME, null, values);
			values.clear();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void getAllSensorData(Context context){
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, null,null,null,null,null,null,"1");
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
		values.put(COL_DATA1, data);
		values.put(COL_TYPE, type);
		values.put(COL_ACCURACY, accuracy);
		values.put(COL_TIMESTAMP, timestamp);
		insertContentValues(context, values);
	}
	public static void insertSensorData(Context context, float[] data, int type, int accuracy, long timestamp){
		ContentValues values = new ContentValues();
		values.put(COL_DATA1, data[0]);
		values.put(COL_DATA2, data[1]);
		values.put(COL_DATA3, data[2]);
		values.put(COL_TYPE, type);
		values.put(COL_ACCURACY, accuracy);
		values.put(COL_TIMESTAMP, timestamp);
		insertContentValues(context, values);
	}

	public static void insertContentValues(Context context, ContentValues values){
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getWritableDatabase();
		try{
			db.insert(TABLE_NAME, null, values);
			values.clear();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static String getSensorData(Context context, int limit){
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, null,null,null,null,null,COL_ID + " DESC",limit+"");
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
	public static ArrayList<SensorData> getSensorData(Context context, int type, long time, long length) {
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(//指定された時間から最新のデータを集める
				TABLE_NAME,
				null,
				COL_TIMESTAMP + " between ? and ? and " +
				"? = " + COL_TYPE,
				new String[]{(time - length) + "", time + "", type + ""},
				null, null,
				COL_ID + " DESC",
				null);

		ArrayList<SensorData> datas = new ArrayList<SensorData>();
		try {
			while (cursor.moveToNext()) {
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
		LearningDBHelper helper = LearninggetInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(//指定された時間から最新のデータを集める
				LearningTABLE_NAME,
				null,
				"? <= " + LearningCOL_TIMESTAMP,
				new String[]{time + ""},
				null, null,
				LearningCOL_TIMESTAMP + " ASC",
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

	public static void writeSensorData2File(Context context){
		Log.e("File kakikomi","7");
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, null,null,null,null,null,null);
		StringBuilder str = new StringBuilder();
		FileOutputStream fos;

		Log.e("File kakikomi","8");
		try {
			Log.e("File kakikomi","9");
			fos = new FileOutputStream(Environment.getExternalStorageDirectory() +
					LogToData.FILE_DIRECTORY +
					LogToData.FILE_NAME,true);
			try {
				Log.e("File kakikomi","10");
				while (cursor.moveToNext()) {
					for(int i = 0;i < cursor.getColumnCount();i++){
						str.append(cursor.getString(i));
						str.append(",");
						//Log.e("database", "[" + cursor.getColumnName(i) + ":" + cursor.getString(i) + "]");
					}
					str.append("\n");
					fos.write(str.toString().getBytes(),0,str.length());
					str.setLength(0);
				}
				//Log.e("database",str.toString());
				Log.e("File kakikomi","11");
			} catch (Exception e) {
				try {
					if (fos!=null){
						fos.close();
					}
				} catch (Exception e2) {}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.e("File kakikomi","12");
	}

	public static int sucoshiWriteSensorData2File(Context context, int id){
		int lastId = id;
		Log.e("File kakikomi","7");
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME,
				null,
				"? < " + COL_ID,
				new String[]{id + ""},
				null, null,
				COL_ID + " ASC",
				"1000");
		StringBuilder str = new StringBuilder();
		FileOutputStream fos;

		Log.e("File kakikomi","8");
		try {
			Log.e("File kakikomi","9");
			fos = new FileOutputStream(Environment.getExternalStorageDirectory() +
					LogToData.FILE_DIRECTORY +
					LogToData.FILE_NAME,true);
			try {
				Log.e("File kakikomi","10");
				while (cursor.moveToNext()) {
					for(int i = 0;i < cursor.getColumnCount();i++){
						str.append(cursor.getString(i));
						str.append(",");
						//Log.e("database", "[" + cursor.getColumnName(i) + ":" + cursor.getString(i) + "]");
					}
					str.append("\n");
					fos.write(str.toString().getBytes(),0,str.length());
					str.setLength(0);
					if(lastId < cursor.getInt(0))	lastId = cursor.getInt(0);
				}
				//Log.e("database",str.toString());
				Log.e("File kakikomi","11");
			} catch (Exception e) {
				try {
					if (fos!=null){
						fos.close();
					}
				} catch (Exception e2) {}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.e("File kakikomi","12");
		return lastId;
	}
	public static int countDats(Context context){
		DBHelper helper = DBHelper.getInstance(context);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, new String[]{"count(*)"},null, null, null, null, null);
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
		return 0;
	}
}
