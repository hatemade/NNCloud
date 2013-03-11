package net.kiknlab.nncloud.service;

import net.kiknlab.nncloud.sensor.SensorAdmin;
import net.kiknlab.nncloud.sensor.StateInference;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class FlyToTheCloud extends Service implements Runnable{
	private final IBinder mBinder = new FTTCBinder();
	private Thread mThread;
	private SensorAdmin mSensor;
	private StateInference mState;
	//private SharedPreferences sp;
	int i = 0;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.e("Sevice Run", "onCreate");

		mSensor = new SensorAdmin(getSystemService(Context.SENSOR_SERVICE), getApplication());
		mSensor.resume();
		
		mState = new StateInference(getApplication());
		
		mThread = new Thread(this);
		mThread.start();
	}

	@Override
	public void run() {
		Log.e("thread test","ok!");
	}

	public String getTest(){//(É–•ÅÕ•)É–πﬁØ¬!!
		return //"â¡ë¨ìxX:" + mSensor.accelerometerValues[0] +
				//"\nY:" + mSensor.accelerometerValues[1] +
				//"\nZ:" + mSensor.accelerometerValues[2] +
				"Y" + Math.floor(Math.toDegrees(mSensor.orientationValues[0])) +
				":X" + Math.floor(Math.toDegrees(mSensor.orientationValues[2])) +
				":Z" + Math.floor(Math.toDegrees(mSensor.orientationValues[1])) +
				":èÛë‘" + mState.state + "ï‡êî" + mState.walkCount +
				":âÒêî" + mSensor.getTimes;
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
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.e("Sevice Run", "onStartCommand:" + i++);
		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mState.stop();
		mSensor.stop();
		Log.e("Sevice Run", "onDestroy");
	}

	@Override
	public void onLowMemory(){
		super.onLowMemory();
		Log.e("Sevice Run", "Low Memoryyyyyyyyyyyyyyyy!");
	}
}
