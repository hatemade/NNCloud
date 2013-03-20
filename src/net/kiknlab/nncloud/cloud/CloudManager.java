package net.kiknlab.nncloud.cloud;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class CloudManager {
	//Serverとの接続の際に共通な定数、俺が、俺たちが定数だ
	public final static String URL_JOINSERVER 		= "http://shaky.cs.dm.u-tokai.ac.jp/~sleepdisorder/nnc/JoinMe.php";
	public final static String URL_LOGINSERVER 		= "http://shaky.cs.dm.u-tokai.ac.jp/~sleepdisorder/nnc/LoginMe.php";
	public final static String URL_SENDSENSORSERVER = "http://shaky.cs.dm.u-tokai.ac.jp/~sleepdisorder/nnc/SendMe.php";
	public final static String JSON_CODE			= "CODE";
	public final static int JSON_CODE_SUCCESS		= 200;
	public final static String DATE_PATTERN			= "yyyy-MM-dd HH:mm:ss";
	public final static String DATETIME_PATTERN		= "yyyy-MM-dd HH:mm:ss.SSS";
	public final static int PREF_SESSION_VALID_TIME	= 1000 * 60 * 60 * 24;
	//JoinServer用
	public final static String PREF_JOINED			= "PREF_JOINED";
	public final static String PREF_NAME			= "PREF_NAME";
	public final static String PREF_PASS			= "PREF_PASS";
	public final static String JSON_NAME			= "NAME";
	public final static String JSON_PASS			= "PASS";
	public final static String JOIN_DIALOG_TITLE	= "Let's joy!";
	//LOGINServer用
	public final static String POST_NAME			= "name";
	public final static String POST_PASS			= "pass";
	public final static String PREF_SESSION			= "PREF_SESSION";
	public final static String PREF_SESSION_TIME	= "PREF_SESSION_TIME";
	public final static String JSON_SESSION			= "SESSION";
	public final static String LOGIN_DIALOG_TITLE	= "Let's login!";
	//SendSensorServer用
	public final static String POST_SESSION			= "session";
	public final static String POST_DATA			= "data";
	public final static String JSON_SUCC			= "SUCC";
	public final static String JSON_FAIL			= "FAIL";
	public final static int POST_LIMIT				= 50;
	public final static String SENSOR_DIALOG_TITLE	= "Let's send sensor!";
	//CloudManager用変数
	/*
	private Context mContext;
	private String user;
	private String pass;
	public boolean isConnected;
	private SharedPreferences sp;
	*/

	//あとでダイアログ消さないと

	public static boolean connectServer(Context context){
		if(checkUserWithoutAdd(context)){
			if(checkSessionWithoutAdd(context)){
				return true;
			}
		}
		return false;
	}

	public static boolean checkUserWithoutAdd(Context context){//ユーザをチェックして、もしなかったら追加するけど、こんなめしょっどでいいんだろうか
		if(checkUser(context))	return true;
		new JoinServerTask(context).execute();
		if(checkUser(context))	return true;
		return false;
	}
	public static boolean checkUser(Context context){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		if(!sp.getBoolean(PREF_JOINED, false) &&
				(sp.getString(PREF_NAME, null) != null ||
				sp.getString(PREF_PASS, null) != null)){
			return true;
		}
		return false;
	}
	public static boolean checkSessionWithoutAdd(Context context){
		if(checkSession(context)) return true;
		new LoginServerTask(context).execute();
		if(checkSession(context)) return true;		
		return false;
	}
	public static boolean checkSession(Context context){//LoginServerTask用に作ったのでstaticメソッド、使わなかったけど
		String sessionTime;
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		if(sp.getString(CloudManager.PREF_SESSION, null) != null){
			if((sessionTime = sp.getString(CloudManager.PREF_SESSION_TIME, null)) != null){
				SimpleDateFormat sdf = new SimpleDateFormat(CloudManager.DATE_PATTERN, Locale.JAPAN);
				try {
					Date sessionDate = sdf.parse(sessionTime);
					if(new Date().getTime() - sessionDate.getTime() < 1000 * 60 * 60 * 23){
						return true;
					}
				} catch (java.text.ParseException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	public static JSONObject getJSONFromServer(HttpPost post){
		final HttpClient client = new DefaultHttpClient();
		JSONObject resultJObj;
		try {
			HttpResponse response = client.execute(post);
			switch(response.getStatusLine().getStatusCode()) {
			case HttpStatus.SC_OK:
				String resultString;
				try {
					resultString = EntityUtils.toString(response.getEntity());
					try {
						resultJObj = new JSONObject(resultString);
						switch(resultJObj.getInt(JSON_CODE)) {
						case JSON_CODE_SUCCESS:
							return resultJObj;
						default:
							break;
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
				break;
			default:
				break;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
