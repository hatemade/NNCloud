package net.kiknlab.nncloud.sensor;

import java.lang.reflect.Array;
import java.util.List;

import net.kiknlab.nncloud.db.LearningDBManager;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class SensorAdmin implements SensorEventListener{
	private final int TYPE_ORIENTATION_MAKE = 3;
	private SensorManager sensorManager;//センサーマネージャ
	private Sensor        accelerometer;//加速度センサー
	//private Sensor        orientation;//回転センサー。諸君らの愛した回転センサーは死んだ、なぜだ！？坊やだからさ
	private Sensor        magnetic;		//磁気センサー
	private Sensor        prox;			//近接センサー
	private Sensor        light;		//光度センサー
	//Orientationがdeprecatedでなければ、こんなに変数いらないじゃない、もう死ぬしかないじゃない
	private boolean IsAccele;	//まず加速度が取得できているか確認します
	private boolean IsMagnetic;	//次に磁気が取得できているか確認します
	/* 回転行列 */
	private static final int MATRIX_SIZE = 16;
	float[]  inR = new float[MATRIX_SIZE];
	float[] outR = new float[MATRIX_SIZE];
	//float[]    I = new float[MATRIX_SIZE];
	/* センサーの値 */
	public float[] orientationValues   = new float[3];
	public int orientationAccuracy;
	public float[] magneticValues      = new float[3];
	public float[] accelerometerValues = new float[3];
	//設定！動的に変更可能にしたい（希望声）
	private int sensorSpeed;
	//計算したりするよう変数？簡潔な言い方だれか考えて(´･ω･`)
	public double sumTime;
	public double preGetTime, nowGetTime;
	public int times;
	//保存する？
	private Context mContext;
	private LearningDBManager mDb;//いらねぇ　追記：ひつようじゃん！
	public int getTimes;
	public static final int TransactionPeriod = 2000;

	public SensorAdmin(Object sensorService, Context context) {
		mContext = context;
		
		//センサスピードの定義、オプションで選べてもいいけど、インターネットから最新の最適な値を取得してもいい
		sensorSpeed = SensorManager.SENSOR_DELAY_FASTEST;

		//センサーマネージャの取得、書き方がCHAOS！うー！にゃー！………ちゃんと書き直そう！気が向いたらな！どうせ初期化時だけだから大した負荷じゃないはずmaybe
		//おなかすいた
		sensorManager=(SensorManager)sensorService;
		List<Sensor> list;
		list=sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (list.size()>0) accelerometer=list.get(0);
		list=sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if (list.size()>0) magnetic=list.get(0);
		list=sensorManager.getSensorList(Sensor.TYPE_PROXIMITY);
		if (list.size()>0) prox=list.get(0);
		list=sensorManager.getSensorList(Sensor.TYPE_LIGHT);
		if (list.size()>0) light=list.get(0);

		//Orientation周りを初期化！
		IsAccele = false;
		IsMagnetic = false;

		//計算なんとかかんとか初期化
		sumTime = 0;
		nowGetTime = preGetTime = java.lang.System.currentTimeMillis();
		times = 0;
		
		//DB初期化して開始！
		mDb = new LearningDBManager();
		mDb.beginTransaction(context);
		getTimes = 0;
	}

	public void resume() {
		//センサーの処理の開始(3)
		if (accelerometer!=null) {
			sensorManager.registerListener(this,accelerometer, sensorSpeed);
		}
		if (magnetic!=null) {
			sensorManager.registerListener(this,magnetic, sensorSpeed);
		}
		if (prox!=null) {
			sensorManager.registerListener(this,prox, sensorSpeed);
		}
		if (light!=null) {
			sensorManager.registerListener(this,light, sensorSpeed);
		}
	}

	//センサーの処理の停止
	public void stop() {
		mDb.endTransaction();
		sensorManager.unregisterListener(this);
	}

	//センサーリスナーの処理
	@Override
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType()){
		case Sensor.TYPE_ACCELEROMETER:
			mDb.insertTransaction(mContext, event.values, event.sensor.getType(), event.accuracy, java.lang.System.currentTimeMillis());
			getTimes++;
			Log.e("acce","");
			accelerometerValues = event.values.clone();
			orientationAccuracy = event.accuracy;
			IsAccele = true;
			IsMagnetic = false;//加速度を取得した直後に取得した磁気でOrientationを計算する
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			mDb.insertTransaction(mContext, event.values, event.sensor.getType(), event.accuracy, java.lang.System.currentTimeMillis());
			getTimes++;
			magneticValues = event.values.clone();
			orientationAccuracy += event.accuracy;
			IsMagnetic = true;
			break;
		case Sensor.TYPE_PROXIMITY:
			mDb.insertTransaction(mContext, event.values[0], event.sensor.getType(), event.accuracy, java.lang.System.currentTimeMillis());
			getTimes++;
			break;
		case Sensor.TYPE_LIGHT:
			mDb.insertTransaction(mContext, event.values[0], event.sensor.getType(), event.accuracy, java.lang.System.currentTimeMillis());
			getTimes++;
			break;
		}
		
		//加速度と磁気使って傾きを作成！
		if(IsAccele&&IsMagnetic){
			SensorManager.getRotationMatrix(inR, null, accelerometerValues, magneticValues);
			SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR);//これでよかったのか？いいんだよな？…これでいい…
			SensorManager.getOrientation(outR, orientationValues);//0がyawで、2がpitchで、1がrollなので注意、Orientationセンサー
			orientationAccuracy = (int)(orientationAccuracy/2);
			IsAccele = false;
			IsMagnetic = false;
			mDb.insertTransaction(mContext, new float[]{orientationValues[0],orientationValues[2],orientationValues[1]}, TYPE_ORIENTATION_MAKE, orientationAccuracy, java.lang.System.currentTimeMillis());
			getTimes++;
		}
		
		//一定回数実行したのちトランザクションを実行
		if(getTimes >= TransactionPeriod){
			mDb.endTransaction();
			getTimes = 0;
			mDb.beginTransaction(mContext);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		Log.e("SensorAdmin","Accuracy Change!!!");
	}
}