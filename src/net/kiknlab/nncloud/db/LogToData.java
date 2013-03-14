package net.kiknlab.nncloud.db;

import java.io.File;
import java.io.FileOutputStream;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

public class LogToData extends AsyncTask<Void, Void, Boolean>{
	private Context mContext;
	private ProgressDialog dialog = null;
	public static final String FILE_NAME= "first.csv";
	public static final String FILE_DIRECTORY = "/NNCloud/";
	private boolean fileExist;
	private int lastId;

	public LogToData(Context context) {
		Log.e("File kakikomi","2");
		lastId = 0;
		fileExist = true;
		File file = new File(Environment.getExternalStorageDirectory() + FILE_DIRECTORY);
		if(!file.exists()){
			Log.e("File kakikomi","2.1");
			if(!file.mkdir())	fileExist = false;
			Log.e("File kakikomi","2.2");
		}
		Log.e("File kakikomi","3");
		if(fileExist){
			Log.e("File kakikomi","3.1");
			file = new File(Environment.getExternalStorageDirectory() + FILE_DIRECTORY + FILE_NAME);
			Log.e("File kakikomi","3.2");
			if(!file.exists()){
				Log.e("File kakikomi","3.3");
				try{if(!file.createNewFile())	fileExist = false;}
				catch(Exception e){}
			}
		}
		Log.e("File kakikomi","4");
		this.mContext = context;
	}

	@Override
	protected void onPreExecute()
	{
		dialog = new ProgressDialog(mContext);
		dialog.setTitle("test");
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.show();
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		Log.e("File kakikomi","5");
		int nextId = lastId + 1;
		if(fileExist){
			while(lastId != nextId){
				nextId = lastId;
				Log.e("File kakikomi","6");
				lastId = LearningDBManager.sucoshiWriteSensorData2File(mContext, lastId);
			}
		}
		Log.e("File kakikomi","13");
		return false;
	}

	@Override
	protected void onPostExecute(Boolean result) 
	{
		dialog.dismiss();
	}
}
