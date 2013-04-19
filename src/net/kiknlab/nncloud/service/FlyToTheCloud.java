package net.kiknlab.nncloud.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.kiknlab.nncloud.R;
import net.kiknlab.nncloud.activity.NNCloudActivity;
import net.kiknlab.nncloud.cloud.CloudManager;
import net.kiknlab.nncloud.cloud.SendMileServerTask;
import net.kiknlab.nncloud.db.StateLogDBManager;
import net.kiknlab.nncloud.sensor.GPSSensor;
import net.kiknlab.nncloud.sensor.SensorAdmin;
import net.kiknlab.nncloud.sensor.StateInference;
import net.kiknlab.nncloud.util.SensorData;
import net.kiknlab.nncloud.util.StateLog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

public class FlyToTheCloud extends Service{
	private final IBinder mBinder = new FTTCBinder();
	private SharedPreferences sp;
	private SensorAdmin mSensor;
	private StateInference mState;
	//2013-04-14 by Pocket7878
	private GPSSensor gps;
	//推定スレッドとマイル送信スレッド
	private final static int SEND_SERVER_THREAD_INTERVAL = 1000 * 60 * 60 * 1;
	public final static String SEND_MILES = "SEND_MILES";
	Timer mSendServerTimer = new Timer();
	private float sendMile;
	final int INFERENCE_THREAD_INTERVAL = 1000 * 5;// msecってmillisecondsとmicrosecondsのどっちだと思う？
	final int NUMBER_OF_VOTE = (int)(60 * 1000)/INFERENCE_THREAD_INTERVAL;//ここで指定された時間の中で最も多い状態が採用される
	final int VOTE_STATE_T = 5/2;
	Timer mInferenceTimer = new Timer();
	ArrayList<Integer> voteState;//各投票数が状態(整数)ごとに入ってる
	ArrayList<Integer> stateList;//投票履歴
	int topState;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.e("Sevice Run", "onCreate");

		mSensor = new SensorAdmin(getSystemService(Context.SENSOR_SERVICE), getApplication());
		mSensor.resume();
		mState = new StateInference(getApplication());
		sp = PreferenceManager.getDefaultSharedPreferences(getApplication());
		sendMile = sp.getFloat(SEND_MILES, mState.mile);

		stateList = new ArrayList<Integer>();
		voteState = new ArrayList<Integer>();
		for(int i = 0;i < StateLog.NUMBER_OF_STATE;i++){voteState.add(0);}
		for(int i = 0;i < NUMBER_OF_VOTE;i++){stateList.add(StateLog.STATE_STOP);}
		voteState.set(StateLog.STATE_STOP, NUMBER_OF_VOTE);
		topState = StateLog.STATE_STOP;
		StateLogDBManager.insertSensorData(getApplication(), new StateLog(StateLog.STATE_LOG_RUNNING, java.lang.System.currentTimeMillis()));

		//2013-04-14 by Pocket7878
		gps = new GPSSensor(getApplication());
		gps.start();

		mInferenceTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				long time = java.lang.System.currentTimeMillis()
						- sp.getLong(StateInference.TIME_LENGTH, StateInference.TIME_LENGTH_DEFAULT);
				if(mSensor.removeAllOldSensorDatas(time)){
					//可能性を推定します
					try{
						Log.e("","" + mSensor.accelerometerDatas.get(0).values[1]);
						Log.e("","" + mSensor.accelerometerDatas.get(10).values[1]);
					} catch ( Exception e){}
					mState.inference(
							(List<SensorData>)mSensor.accelerometerDatas.clone(),
							(List<SensorData>)mSensor.orientationDatas.clone());
					//推定した結果をもとに投票開始！
					voteState.set(mState.stateLog.state, voteState.get(mState.stateLog.state) + 1);
					voteState.set(stateList.get(0), voteState.get(stateList.get(0)) - 1);
					stateList.add(mState.stateLog.state);
					stateList.remove(0);
					Log.e("votes", "" + voteState.get(topState));
					if(voteState.get(topState) < voteState.get(mState.stateLog.state)){
						Log.e("change", "state:" + mState.stateLog.state);
						topState = mState.stateLog.state;
						StateLogDBManager.insertSensorData(getApplication(), mState.stateLog);
					}
				}
			}
		}, 0, INFERENCE_THREAD_INTERVAL);
		mSendServerTimer.schedule(new TimerTask() {
			private Handler mHandler = new Handler(Looper.getMainLooper());
			@Override
			public void run() {
				mHandler.post(new Runnable() {
					public void run(){
						if(CloudManager.connectServer(getApplication())){
							new SendMileServerTask(getApplication()).execute(new Float[]{mState.mile, sendMile});
						}
					}
				});
			}
		}, 0, SEND_SERVER_THREAD_INTERVAL);
	}

	public String getTest(){//(σ･∀･)σｹﾞｯﾂ!!
		return //"加速度X:" + mSensor.accelerometerValues[0] +
				//"\nY:" + mSensor.accelerometerValues[1] +
				//"\nZ:" + mSensor.accelerometerValues[2] +
				//"Y" + Math.floor(Math.toDegrees(mSensor.orientationValues[0])) +
				//":X" + Math.floor(Math.toDegrees(mSensor.orientationValues[2])) +
				//":Z" + Math.floor(Math.toDegrees(mSensor.orientationValues[1])) +
				//":状態" + mState.stateLog.getStateString() +
				//":歩数" + mState.numSteps +
				//":マイル" + mState.mile;
				mState.stateLog.state +
				":" + mState.numSteps +
				":" + mState.mile;
	}

	public class FTTCBinder extends Binder {
		FlyToTheCloud getService() {
			return FlyToTheCloud.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// 解放せよ！われわれに働く気はない！負けだと思う！
		mState.stop();
		mSensor.stop();
		gps.stop();
		mInferenceTimer.cancel();
		mInferenceTimer = null;
		mSendServerTimer.cancel();
		mSendServerTimer = null;
		StateLogDBManager.insertSensorData(getApplication(), new StateLog(StateLog.STATE_LOG_STOPPED, java.lang.System.currentTimeMillis()));
		// 解放されました、今日から無職です、メモリはフリーターみたいな、フリーな領域みたいな
	}

	private void setNotifyCation(){
		//Notificationインスタンスの生成と設定
		Notification notify = new Notification();
		notify.icon = R.drawable.walking;
		notify.tickerText = "ティッカーテキスト";
		notify.number = 2;
		try{
			SimpleDateFormat date = new SimpleDateFormat("yy/mm/dd HH:mm");
			notify.when = date.parse("2010/5/20").getTime();
		}catch(Exception e){
			notify.when = System.currentTimeMillis();
		}

		Intent i = new Intent(getApplicationContext(), NNCloudActivity.class);
		PendingIntent pend = PendingIntent.getActivity(this, 0, i, 0);
		//notify.;
		//.setLatestEventInfo(getApplicationContext(), "", "", pend);
		//	.setLatestEventInfo(getApplicationContext(), "タイトル", "テキスト", setIntent() );

		NotificationManager mManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		mManager.notify(1, notify);
	}
}
