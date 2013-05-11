package net.kiknlab.nncloud.cloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import net.kiknlab.nncloud.db.LearningDBManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

public class SendInferenceLogServerTask extends AsyncTask<Void, Void, Boolean>{
	private Context mContext;
	private SharedPreferences sp;
	private static final String FILE_NAME= "inference_log.csv";
	private static final String FILE_DIRECTORY = "/NNCloud/";
	private boolean fileExist;

	public SendInferenceLogServerTask(Context context) {
		sp = PreferenceManager.getDefaultSharedPreferences(context);
		this.mContext = context;
	}

	@Override
	protected void onPreExecute()
	{
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		final HttpPost post = new HttpPost(CloudManager.URL_SENDINFERENCELOGSERVER);
		String data = "";

		String session = sp.getString(CloudManager.PREF_SESSION, null);
		if(session == null)	return false;
		
		//ログが大きすぎた場合の処理を書く, TODO
		data = file2data(Environment.getExternalStorageDirectory() + FILE_DIRECTORY + FILE_NAME).replaceAll("\n", "#");
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair(CloudManager.POST_SESSION, session));
		postParams.add(new BasicNameValuePair(CloudManager.POST_DATA, data));
		try {
			post.setEntity(new UrlEncodedFormEntity(postParams));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		JSONObject resultJObj = CloudManager.getJSONFromServer(post);
		if(resultJObj != null){
			try {
				Log.e("AAAAA", "" + fileClean(Environment.getExternalStorageDirectory() + FILE_DIRECTORY + FILE_NAME));
				if(resultJObj.getInt(CloudManager.JSON_SUCC) <= 0)	return false;
				if(resultJObj.getInt(CloudManager.JSON_FAIL) >= 1)	return false;
				return true;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	protected void onPostExecute(Boolean res) 
	{
	}
	
	private static boolean fileClean(String fileName){
		File file = new File(fileName);
		if(!file.exists()){
			return false;
		}
		else{
			file.delete();
			boolean fileExist = true;
			file = new File(Environment.getExternalStorageDirectory() + FILE_DIRECTORY);
			if(!file.exists()){
				if(!file.mkdir())	fileExist = false;
			}
			if(fileExist){
				file = new File(Environment.getExternalStorageDirectory() + FILE_DIRECTORY + FILE_NAME);
				if(!file.exists()){
					try{if(!file.createNewFile())	fileExist = false;}
					catch(Exception e){fileExist = false;}
				}
			}
			return fileExist;
		}
	}
	
	//ファイル→バイトデータ
	private static String file2data(String fileName){
		FileInputStream fileInputStream;
		String readString = "";
		try {
			fileInputStream = new FileInputStream(fileName);
			byte[] readBytes = new byte[fileInputStream.available()];
			fileInputStream.read(readBytes);
			readString = new String(readBytes);
		}catch (Exception e){
		}
		return readString;
	}
}
