package net.kiknlab.nncloud.cloud;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class LoginServerTask extends AsyncTask<Void, Void, Boolean>{
	private SharedPreferences sp;

	public LoginServerTask(Context context) {
		sp = PreferenceManager.getDefaultSharedPreferences(context);
	}

	@Override
	protected void onPreExecute()
	{
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		final HttpPost post = new HttpPost(CloudManager.URL_LOGINSERVER);

		//POSTのせってーい、めどい、メソッドにしておくべき？めんどーい
		String name = sp.getString(CloudManager.PREF_NAME, null);
		String pass = sp.getString(CloudManager.PREF_PASS, null);
		if(name == null||pass == null)	return false;
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair(CloudManager.POST_NAME, name));
		postParams.add(new BasicNameValuePair(CloudManager.POST_PASS, pass));
		try {
			post.setEntity(new UrlEncodedFormEntity(postParams));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return false;
		}

		JSONObject resultJObj = CloudManager.getJSONFromServer(post);
		if(resultJObj != null){
			try {
				if(resultJObj.getString(CloudManager.JSON_SESSION).length() < 32)	return false;
				SimpleDateFormat sdf = new SimpleDateFormat(CloudManager.DATE_PATTERN, Locale.JAPAN);
				sp.edit().putString(CloudManager.PREF_SESSION, resultJObj.getString(CloudManager.JSON_SESSION)).commit();
				sp.edit().putString(CloudManager.PREF_SESSION_TIME, sdf.format(new Date())).commit();
				return true;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	protected void onPostExecute(Boolean result) 
	{
	}
}
