package net.kiknlab.nncloud.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LearningDBHelper extends SQLiteOpenHelper {
	//êÌèpÇÕëäéËÇ∆ÇÃêÌóÕç∑Çï¢Ç∑
	//êÌó™ÇÕÇªÇÃìyë‰Ç©ÇÁï¢Ç∑
	private static LearningDBHelper instance = null;
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "NNCloud";
	static final String TABLE_NAME = "sensor_datas";
	static final String COL_ID = "id";
	static final String COL_DATA1 = "data1";
	static final String COL_DATA2 = "data2";
	static final String COL_DATA3 = "data3";
	static final String COL_TYPE = "type";
	static final String COL_ACCURACY = "accuracy";
	static final String COL_TIMESTAMP = "timestamp";

	public static final String CREATE_LEARNING_TABLE_SQL = "create table " + 
			TABLE_NAME + "(" +
			COL_ID + " integer primary key autoincrement," +
			COL_DATA1 + " real not null," +
			COL_DATA2 + " real," +
			COL_DATA3 + " real," +
			COL_TYPE + " integer not null," +
			COL_ACCURACY + " integer not null," +
			COL_TIMESTAMP + " text not null" +
			")";

	public static final String DROP_LEARNING_TABLE_SQL = "drop table if exists " + TABLE_NAME;

	private LearningDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public static synchronized LearningDBHelper getInstance(Context context) {
		if (instance == null) {
			instance = new LearningDBHelper(context);
		}
		return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_LEARNING_TABLE_SQL);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(DROP_LEARNING_TABLE_SQL);
		onCreate(db);
	}
}
