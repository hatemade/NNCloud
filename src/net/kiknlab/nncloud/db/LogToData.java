package net.kiknlab.nncloud.db;

import java.io.File;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

public class LogToData extends AsyncTask<Void, Void, Boolean>{
	private Context mContext;
	private ProgressDialog dialog = null;
	public static final String FILE_NAME= "first.csv";
	public static final String FILE_DIRECTORY = "/NNCloud/";
	private boolean fileExist;
	private int lastId;

	public LogToData(Context context) {
		lastId = 0;
		fileExist = true;
		File file = new File(Environment.getExternalStorageDirectory() + FILE_DIRECTORY);
		if(!file.exists()){
			if(!file.mkdir())	fileExist = false;
		}
		if(fileExist){
			file = new File(Environment.getExternalStorageDirectory() + FILE_DIRECTORY + FILE_NAME);
			if(!file.exists()){
				try{if(!file.createNewFile())	fileExist = false;}
				catch(Exception e){}
			}
		}
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
		int nextId = lastId + 1;
		if(fileExist){
			while(lastId != nextId){
				nextId = lastId;
				lastId = LearningDBManager.sucoshiWriteSensorData2File(mContext, lastId);
			}
			LearningDBManager.cleanUpTable(mContext);
			return true;
		}
		return false;
	}

	@Override
	protected void onPostExecute(Boolean result) 
	{
		dialog.dismiss();
	}
}
