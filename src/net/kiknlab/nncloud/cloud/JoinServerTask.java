package net.kiknlab.nncloud.cloud;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class JoinServerTask extends AsyncTask<Void, Void, Boolean>{
	private SharedPreferences sp;

	public JoinServerTask(Context context) {
		sp = PreferenceManager.getDefaultSharedPreferences(context);
	}

	@Override
	protected void onPreExecute()
	{
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		final HttpPost post = new HttpPost(CloudManager.URL_JOINSERVER);
		JSONObject resultJObj = CloudManager.getJSONFromServer(post);
		if(resultJObj != null){
			try {
				if(resultJObj.getString(CloudManager.JSON_NAME).length() < 6)	return false;
				if(resultJObj.getString(CloudManager.JSON_PASS).length() < 6)	return false;
				sp.edit().putString(CloudManager.PREF_NAME, resultJObj.getString(CloudManager.JSON_NAME)).commit();
				sp.edit().putString(CloudManager.PREF_PASS, resultJObj.getString(CloudManager.JSON_PASS)).commit();
				sp.edit().putBoolean(CloudManager.PREF_JOINED, true).commit();
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
}
