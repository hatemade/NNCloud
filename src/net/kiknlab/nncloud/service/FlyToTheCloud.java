package net.kiknlab.nncloud.service;

import java.util.Timer;
import java.util.TimerTask;

import net.kiknlab.nncloud.sensor.SensorAdmin;
import net.kiknlab.nncloud.sensor.StateInference;
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
	private final static int THREAD_INTERVAL = 1000 * 60 * 60 * 1;
	private Thread mThread;
	private SensorAdmin mSensor;
	private StateInference mState;
	final int INTERVAL_PERIOD = 1000 * 5;// msecÇ¡ÇƒmillisecondsÇ∆microsecondsÇÃÇ«Ç¡ÇøÇæÇ∆évÇ§ÅH
	Timer mTimer = new Timer();
	private SharedPreferences sp;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.e("Sevice Run", "onCreate");

		mSensor = new SensorAdmin(getSystemService(Context.SENSOR_SERVICE), getApplication());
		mSensor.resume();
		mState = new StateInference(getApplication());
		sp = PreferenceManager.getDefaultSharedPreferences(getApplication());
		
		mTimer.schedule(new TimerTask() {
			int acceleIndex, orientationIndex;
			@Override
			public void run() {
				acceleIndex = mSensor.accelerometerDatas.size();
				orientationIndex = mSensor.orientationDatas.size();
				long time = java.lang.System.currentTimeMillis() - sp.getLong(StateInference.TIME_LENGTH, StateInference.TIME_LENGTH_DEFAULT);
				mSensor.removeAllOldSensorDatas(time);
				//mState.judge(
				//		acceleIndex, mSensor.accelerometerDatas, mSensor.orientationDatas, null
				//		);
			}
		}, 0, INTERVAL_PERIOD);
		mThread = new Thread(new Thread(){
			@Override
			public void run(){
				
			}
		});
		mThread.start();
	}

	public String getTest(){//(É–•ÅÕ•)É–πﬁØ¬!!
		return //"â¡ë¨ìxX:" + mSensor.accelerometerValues[0] +
				//"\nY:" + mSensor.accelerometerValues[1] +
				//"\nZ:" + mSensor.accelerometerValues[2] +
				"Y" + Math.floor(Math.toDegrees(mSensor.orientationValues[0])) +
				":X" + Math.floor(Math.toDegrees(mSensor.orientationValues[2])) +
				":Z" + Math.floor(Math.toDegrees(mSensor.orientationValues[1])) +
				":èÛë‘" + mState.state +
				":ï‡êî" + mState.numSteps;
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
		mState.stop();
		mSensor.stop();
		mThread = null;
		Log.e("Sevice Run", "onDestroy");
	}
}
