package net.kiknlab.nncloud.sensor;

import java.util.ArrayList;
import java.util.List;

import net.kiknlab.nncloud.db.LearningDBManager;
import net.kiknlab.nncloud.util.SensorData;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorAdmin implements SensorEventListener{
	public final static int TYPE_ORIENTATION_MAKE = 3;//orientationがdeprecatedなので自分で定義
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
	public float[] accelerometerValues   = new float[3];
	public float[] magneticValues   = new float[3];
	public float[] orientationValues   = new float[3];
	public int orientationAccuracy;
	public ArrayList<SensorData> accelerometerDatas;
	public ArrayList<SensorData> magneticDatas;
	public ArrayList<SensorData> orientationDatas;
	//設定！動的に変更可能にしたい（希望声）
	private int sensorSpeed;
	//保存する？
	//private Context mContext;
	//private LearningDBManager mDb;//いらねぇ　追記：ひつようじゃん！ 追記:いらなかった
	//public static final int TransactionPeriod = 800;

	public SensorAdmin(Object sensorService, Context context) {
		//mContext = context;

		//センサスピードの定義、オプションで選べてもいいけど、インターネットから最新の最適な値を取得してもいい、してみたい
		//よくよく見てみたら、ディレイの速さintで設定できるのかmicrosecかー、ん？まいくろ？
		//なんか、他のアプリでセンサー取得してると、そっちで設定された速さが反映される？用検証
		sensorSpeed = 100000;//SensorManager.SENSOR_DELAY_FASTEST;

		//センサーマネージャの取得、書き方がCHAOS！うー！にゃー！………ちゃんと書き直そう！気が向いたらな！どうせ初期化時だけだから大した負荷じゃないはずmaybe
		//おなかすいた
		sensorManager=(SensorManager)sensorService;
		List<Sensor> list;
		list=sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (list.size()>0) accelerometer=list.get(0);
		list=sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if (list.size()>0) magnetic=list.get(0);
		/*
		list=sensorManager.getSensorList(Sensor.TYPE_PROXIMITY);
		if (list.size()>0) prox=list.get(0);
		list=sensorManager.getSensorList(Sensor.TYPE_LIGHT);
		if (list.size()>0) light=list.get(0);*/

		//Orientation周りを初期化！
		IsAccele = false;
		IsMagnetic = false;

		//Sensorの値を格納するArrayList
		accelerometerDatas = new ArrayList<SensorData>();
		magneticDatas = new ArrayList<SensorData>();
		orientationDatas = new ArrayList<SensorData>();
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
		sensorManager.unregisterListener(this);
	}

	//センサーリスナーの処理
	@Override
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType()){
		case Sensor.TYPE_ACCELEROMETER:
			accelerometerDatas.add(new SensorData(event.values, event.sensor.getType(), event.accuracy, java.lang.System.currentTimeMillis()));
			//Log.e("values2", "" + accelerometerDatas.get(0).values[1]);//accelerometerDatas.size() - 1).values[1]);
			accelerometerValues = event.values;
			orientationAccuracy = event.accuracy;
			IsAccele = true;
			IsMagnetic = false;//加速度を取得した直後に取得した磁気でOrientationを計算する
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			magneticDatas.add(new SensorData(event.values, event.sensor.getType(), event.accuracy, java.lang.System.currentTimeMillis()));
			magneticValues = event.values;
			orientationAccuracy += event.accuracy;
			IsMagnetic = true;
			break;
		case Sensor.TYPE_PROXIMITY:
			break;
		case Sensor.TYPE_LIGHT:
			break;
		}

		//加速度と磁気使って傾きを作成！
		if(IsAccele&&IsMagnetic){
			SensorManager.getRotationMatrix(inR, null, accelerometerValues, magneticValues);
			SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR);//これでよかったのか？いいんだよな？…これでいい…
			SensorManager.getOrientation(outR, orientationValues);//0がyawで、2がpitchで、1がrollなので注意、Orientationセンサー
			orientationAccuracy = (int)(orientationAccuracy/2);
			orientationDatas.add(new SensorData(orientationValues, TYPE_ORIENTATION_MAKE, orientationAccuracy, java.lang.System.currentTimeMillis()));
			IsAccele = false;
			IsMagnetic = false;
		}
	}

	public boolean removeAllOldSensorDatas(long time){
		try{
			removeOldSensorDatas(accelerometerDatas, time);
			removeOldSensorDatas(magneticDatas, time);
			removeOldSensorDatas(orientationDatas, time);
			return true;
		}
		catch(NullPointerException e){
			return false;
		}
	}

	public void removeOldSensorDatas(ArrayList<SensorData> Datas, long time) throws NullPointerException{
		int index = -1;
		for(int i = Datas.size() - 1;i >= 0;i--){
			if(Datas.get(i).timestamp < time){
				index = i;
				break;
			}
		}
		for(int i = index;i >= 0;i--){
			Datas.remove(i);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		//Log.e("SensorAdmin","Accuracy Change!!!");
	}
}