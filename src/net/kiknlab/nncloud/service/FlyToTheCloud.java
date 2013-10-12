package net.kiknlab.nncloud.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.kiknlab.nncloud.R;
import net.kiknlab.nncloud.activity.NNCloudActivity;
import net.kiknlab.nncloud.cloud.CloudManager;
import net.kiknlab.nncloud.cloud.SendInferenceLogServerTask;
import net.kiknlab.nncloud.cloud.SendMileServerTask;
import net.kiknlab.nncloud.db.DayLogDBManager;
import net.kiknlab.nncloud.db.StateLogDBManager;
import net.kiknlab.nncloud.sensor.GPSSensor;
import net.kiknlab.nncloud.sensor.SensorAdmin;
import net.kiknlab.nncloud.sensor.StateInference;
import net.kiknlab.nncloud.util.SensorData;
import net.kiknlab.nncloud.util.StateLog;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
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
	private Timer mSendServerTimer = new Timer();
	private float sendMile;
	public float mile;//みゃいるじゃないよ？
	public static final float	POWER_SAVING_STAIR = 0.74f;//階段利用時の節電量。単位は…？なんだっけ?ワット?
	public static final float	POWER_USING_ELEVATOR = -3.33f;//エレベータ利用時の消費電力
	public static final String MILEAGE_POINT = "MILAGE_POINT";
	public final int INFERENCE_THREAD_INTERVAL = 5 * 1000;// msecってmillisecondsとmicrosecondsのどっちだと思う？
	private Timer mInferenceTimer = new Timer();
	public final int NUMBER_OF_VOTE = (int)((2 * 5 * 1000)/INFERENCE_THREAD_INTERVAL);//ここで指定された時間の中で最も多い状態が採用される
	private ArrayList<Integer> voteState;//各投票数が状態(整数)ごとに入ってる
	private ArrayList<Integer> stateList;//投票履歴
	public int topState;
	public long inferenceTime;
	public final long ALLOW_DIFFERENT_INFERENCE_TIME = INFERENCE_THREAD_INTERVAL/2;//推定の間隔五秒からどこまでのずれを許容するか
	private boolean isStop;

	@Override
	public void onCreate() {
		super.onCreate();
		isStop = false;
		Log.e("Sevice Run", "onCreate");

		mSensor = new SensorAdmin(getSystemService(Context.SENSOR_SERVICE), getApplication());
		mSensor.resume();
		mState = new StateInference(getApplication());
		sp = PreferenceManager.getDefaultSharedPreferences(getApplication());
		sendMile = sp.getFloat(SEND_MILES, mile);

		stateList = new ArrayList<Integer>();
		voteState = new ArrayList<Integer>();
		for(int i = 0;i < StateLog.NUMBER_OF_STATE;i++){voteState.add(0);}
		for(int i = 0;i < NUMBER_OF_VOTE;i++){stateList.add(StateLog.STATE_STOP);}
		voteState.set(StateLog.STATE_STOP, NUMBER_OF_VOTE);
		topState = StateLog.STATE_STOP;
		Log.e("FTTC","1");
		if(!StateLogDBManager.lastStateIsStop(getApplication())){
			Log.e("FTTC","2");
			StateLogDBManager.insertSensorData(getApplication(),
				new StateLog(StateLog.STATE_LOG_ABNORMAL_STOP,
				java.lang.System.currentTimeMillis()));
		}
		Log.e("FTTC","3");
		StateLogDBManager.insertSensorData(getApplication(),
				new StateLog(StateLog.STATE_LOG_RUNNING,
				java.lang.System.currentTimeMillis()));

		//2013-04-14 by Pocket7878
		//gps = new GPSSensor(getApplication());
		//gps.start();

		mile = sp.getFloat(MILEAGE_POINT, 0);
		inferenceTime = java.lang.System.currentTimeMillis();
		mInferenceTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				long time = java.lang.System.currentTimeMillis()
						- sp.getLong(StateInference.TIME_LENGTH, StateInference.TIME_LENGTH_DEFAULT);
				if(mSensor.removeAllOldSensorDatas(time)){
					//可能性を推定します
					mState.inference(
							(List<SensorData>)mSensor.accelerometerDatas.clone(),
							(List<SensorData>)mSensor.orientationDatas.clone());
					//推定した結果をもとに投票開始！
					voteState.set(mState.stateLog.state, voteState.get(mState.stateLog.state) + 1);
					voteState.set(stateList.get(0), voteState.get(stateList.get(0)) - 1);
					stateList.add(mState.stateLog.state);
					stateList.remove(0);
					if(voteState.get(topState) < voteState.get(mState.stateLog.state)){
						topState = mState.stateLog.state;
						StateLogDBManager.insertSensorData(getApplication(), mState.stateLog);
					}
					
					calcMile();
				}
			}
			public void calcMile(){//absしなくていいんじゃね？いや、必要だ
				if(Math.abs(java.lang.System.currentTimeMillis()
						- inferenceTime - INFERENCE_THREAD_INTERVAL)
						< ALLOW_DIFFERENT_INFERENCE_TIME){
					if(mState.stateLog.state == StateLog.STATE_STAIR){
						mile += POWER_SAVING_STAIR * ((java.lang.System.currentTimeMillis() - inferenceTime) / 1000);
					} else if(mState.stateLog.state == StateLog.STATE_ELEVATOR){
						mile += POWER_USING_ELEVATOR * ((java.lang.System.currentTimeMillis() - inferenceTime) / 1000);
					}
					sp.edit().putFloat(MILEAGE_POINT, mile).commit();
				}
				inferenceTime = java.lang.System.currentTimeMillis();
			}
		}, 0, INFERENCE_THREAD_INTERVAL);
		
		mSendServerTimer.schedule(new TimerTask() {
			private Handler mHandler = new Handler(Looper.getMainLooper());
			@Override
			public void run() {
				mHandler.post(new Runnable() {
					public void run(){
						int insertStep = mState.stackStep;
						mState.stackStep = 0;
						DayLogDBManager.insertStepLog(getApplication(), insertStep, java.lang.System.currentTimeMillis());
						if(CloudManager.connectServer(getApplication())){
							new SendMileServerTask(getApplication()).execute(new Float[]{mile, sendMile});
							new SendInferenceLogServerTask(getApplication()).execute();
						}
					}
				});
			}
		}, 0, SEND_SERVER_THREAD_INTERVAL);

		setNotifycation();
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
				":" + mile;
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
		stopService();
	}
	
	public void stopService(){
		if(!isStop){
			// 解放せよ！われわれに働く気はない！負けだと思う！
			int insertStep = mState.stackStep;
			mState.stackStep = 0;
			DayLogDBManager.insertStepLog(getApplication(), insertStep, java.lang.System.currentTimeMillis());
			sp.edit().putFloat(MILEAGE_POINT, mile).commit();
			mState.stop();
			mSensor.stop();
			//gps.stop();
			mInferenceTimer.cancel();
			mInferenceTimer = null;
			mSendServerTimer.cancel();
			mSendServerTimer = null;
			StateLogDBManager.insertSensorData(getApplication(), new StateLog(StateLog.STATE_LOG_STOPPED, java.lang.System.currentTimeMillis()));
			cancelNotifycation();
			isStop = true;
			// 解放されました、今日から無職です、メモリはフリーターみたいな、フリーな領域みたいな
		}
	}

	private void setNotifycation(){
		Intent i = new Intent(getApplicationContext(), NNCloudActivity.class);
		PendingIntent pend = PendingIntent.getActivity(this, 0, i, 0);

		Notification notify = new NotificationCompat.Builder(this)
		.setContentTitle("NNCloud")
		.setContentText("running")
		.setSmallIcon(R.drawable.walk)
		.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.walking))
		.setContentIntent(pend)
		.build();

		NotificationManager mManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		mManager.notify(1, notify);
	}

	private void cancelNotifycation(){
		NotificationManager notifyManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notifyManager.cancel(1);
	}
}
