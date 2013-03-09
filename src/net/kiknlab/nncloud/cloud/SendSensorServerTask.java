package net.kiknlab.nncloud.cloud;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import android.preference.PreferenceManager;
import android.util.Log;

public class SendSensorServerTask extends AsyncTask<Void, Void, Boolean>{
	private Context mContext;
	private ProgressDialog dialog = null;
	private SharedPreferences sp;

	public SendSensorServerTask(Context context) {
		sp = PreferenceManager.getDefaultSharedPreferences(context);
		this.mContext = context;
	}

	@Override
	protected void onPreExecute()
	{
		dialog = new ProgressDialog(mContext);
		dialog.setTitle(CloudManager.SENSOR_DIALOG_TITLE);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.show();
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		final HttpPost post = new HttpPost(CloudManager.URL_SENDSENSORSERVER);
		String data = "";

		sp.edit().commit();
		String session = sp.getString(CloudManager.PREF_SESSION, null);
		if(session == null)	return false;
		data += "10,20,30,1,2,2013-02-24 21:00:00.001#";//ÇŸÇÒÇ∆ÇÕÇ±Ç±Ç≈Ç≈Å[ÇΩÇ◊Å[Ç∑Ç≥ÇÒÇ…Ç®ÇÀÇ™Ç¢ÇµÇƒÇ≈Å[ÇΩÇÇ‡ÇÁÇ¢Ç‹Ç∑
		data += "11,20,30,1,2,2013-02-24 21:00:00.002#";
		data += "10,22,30,1,2,2013-02-24 21:00:00.003#";
		data += "10,20,33,1,2,2013-02-24 21:00:00.004#";
		data += "14,24,34,1,2,2013-02-24 21:00:00.005";
		LearningDBManager.getSensorData(mContext, CloudManager.POST_LIMIT);
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
		dialog.dismiss();
	}
}
