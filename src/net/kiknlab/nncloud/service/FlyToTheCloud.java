package net.kiknlab.nncloud.service;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.kiknlab.nncloud.cloud.CloudManager;
import net.kiknlab.nncloud.cloud.SendMileServerTask;
import net.kiknlab.nncloud.sensor.SensorAdmin;
import net.kiknlab.nncloud.sensor.StateInference;
import net.kiknlab.nncloud.util.SensorData;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class FlyToTheCloud extends Service{
	private final IBinder mBinder = new FTTCBinder();
	private SharedPreferences sp;
	private SensorAdmin mSensor;
	private StateInference mState;
	//推定スレッドとマイル送信スレッド
	private final static int SEND_SERVER_THREAD_INTERVAL = 1000 * 60 * 60 * 1;
	public final static String SEND_MILES = "SEND_MILES";
	Timer mSendServerTimer = new Timer();
	private float sendMile;
	final int INFERENCE_THREAD_INTERVAL = 1000 * 5;// msecってmillisecondsとmicrosecondsのどっちだと思う？
	Timer mInferenceTimer = new Timer();
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.e("Sevice Run", "onCreate");

		mSensor = new SensorAdmin(getSystemService(Context.SENSOR_SERVICE), getApplication());
		mSensor.resume();
		mState = new StateInference(getApplication());
		sp = PreferenceManager.getDefaultSharedPreferences(getApplication());
		sendMile = sp.getFloat(SEND_MILES, mState.mile);
		
		mInferenceTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				long time = java.lang.System.currentTimeMillis()
						- sp.getLong(StateInference.TIME_LENGTH, StateInference.TIME_LENGTH_DEFAULT);
				mSensor.removeAllOldSensorDatas(time);
				Log.e("a",time + "");
				mState.inference(
						(List<SensorData>)mSensor.accelerometerDatas.clone(),
						(List<SensorData>)mSensor.orientationDatas.clone());
			}
		}, 0, INFERENCE_THREAD_INTERVAL);
		mSendServerTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if(CloudManager.connectServer(getApplication())){
					new SendMileServerTask(getApplication()).execute(new Float[]{mState.mile, sendMile});
				}
			}
		}, 0, SEND_SERVER_THREAD_INTERVAL);
	}

	public String getTest(){//(σ･∀･)σｹﾞｯﾂ!!
		return //"加速度X:" + mSensor.accelerometerValues[0] +
				//"\nY:" + mSensor.accelerometerValues[1] +
				//"\nZ:" + mSensor.accelerometerValues[2] +
				"Y" + Math.floor(Math.toDegrees(mSensor.orientationValues[0])) +
				":X" + Math.floor(Math.toDegrees(mSensor.orientationValues[2])) +
				":Z" + Math.floor(Math.toDegrees(mSensor.orientationValues[1])) +
				":状態" + mState.state +
				":歩数" + mState.numSteps +
				":マイル" + mState.mile;
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
		mInferenceTimer.cancel();
		mInferenceTimer = null;
		mSendServerTimer.cancel();
		mSendServerTimer = null;
		// 解放されました、今日から無職です、メモリはフリーターみたいな、フリーな領域みたいな
	}
}
