package net.kiknlab.nncloud.cloud;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import net.kiknlab.nncloud.db.LearningDBManager;
import net.kiknlab.nncloud.service.FlyToTheCloud;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class SendMileServerTask extends AsyncTask<Float, Void, Boolean>{
	private Context mContext;
	private SharedPreferences sp;
	private float mile;
	private float sendMile;

	public SendMileServerTask(Context context) {
		sp = PreferenceManager.getDefaultSharedPreferences(context);
		this.mContext = context;
	}

	@Override
	protected void onPreExecute()
	{
	}

	@Override
	protected Boolean doInBackground(Float... params) {
		final HttpPost post = new HttpPost(CloudManager.URL_SENDMILESERVER);
		mile = params[0];
		sendMile = params[1];

		String session = sp.getString(CloudManager.PREF_SESSION, null);
		if(session == null)	return false;
		LearningDBManager.getSensorData(mContext, CloudManager.POST_LIMIT);
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair(CloudManager.POST_SESSION, session));
		postParams.add(new BasicNameValuePair(CloudManager.POST_MILE, (mile - sendMile) + ""));
		try {
			post.setEntity(new UrlEncodedFormEntity(postParams));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		JSONObject resultJObj = CloudManager.getJSONFromServer(post);
		if(resultJObj != null){
			return true;
		}
		return false;
	}

	@Override
	protected void onPostExecute(Boolean res) 
	{
		if(res){
			sp.edit().putFloat(FlyToTheCloud.SEND_MILES, mile).commit();
		}
	}
}
