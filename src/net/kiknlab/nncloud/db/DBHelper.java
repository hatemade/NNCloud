package net.kiknlab.nncloud.db;

import net.kiknlab.nncloud.service.FlyToTheCloud;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
	//êÌèpÇÕëäéËÇ∆ÇÃêÌóÕç∑Çï¢Ç∑
	//êÌó™ÇÕÇªÇÃìyë‰Ç©ÇÁï¢Ç∑
	private static DBHelper instance = null;
	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "NNCloud";

	private DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public static synchronized DBHelper getInstance(Context context) {
		if (instance == null) {
			instance = new DBHelper(context);
		}
		return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(LearningDBManager.CREATE_TABLE_SQL);
		db.execSQL(StateLogDBManager.CREATE_TABLE_SQL);
		db.execSQL(LocationLogDBManager.CREATE_TABLE_SQL);
		db.execSQL(DayLogDBManager.CREATE_TABLE_SQL);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch(newVersion){
		case 2:
			db.execSQL(DayLogDBManager.CREATE_TABLE_SQL);
			break;
		default:
			db.execSQL(LearningDBManager.DROP_TABLE_SQL);
			db.execSQL(StateLogDBManager.DROP_TABLE_SQL);
			db.execSQL(LocationLogDBManager.DROP_TABLE_SQL);
			db.execSQL(DayLogDBManager.DROP_TABLE_SQL);
			onCreate(db);
			break;
		}
	}
}
