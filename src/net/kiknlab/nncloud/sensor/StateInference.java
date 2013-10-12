package net.kiknlab.nncloud.sensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import net.kiknlab.nncloud.cloud.CloudManager;
import net.kiknlab.nncloud.db.DayLogDBManager;
import net.kiknlab.nncloud.util.SensorData;
import net.kiknlab.nncloud.util.StateLog;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class StateInference {//状態推定
	SharedPreferences sp;
	Context mContext;
	// 推定変数
	public StateLog stateLog;
	private boolean inElevator;
	public int numSteps;//とりあえず一日の歩数をほじ
	public int stackStep;//未挿入の歩数
	public long dayWalk;

	public String debugLog;
	public boolean fileExist;
	public int debugId;
	public static final String FILE_NAME= "inference_log.csv";
	public static final String FILE_DIRECTORY = "/NNCloud/";

	// スレッド三回回ってわんわん
	//final int INTERVAL_PERIOD = 20000;// msecかと思ったか，あたりだよ！もう一本！millisecondsです…はい…
	//Timer timer = new Timer();

	//後から追加したので、あとから整理する
	//閾値は時間をもとに増減させたいなら、個々のデフォルトに時間をかければいいかなぁ
	public static final String	TIME_LENGTH							= "TIME_LENGTH";
	public static final long	TIME_LENGTH_DEFAULT					= 5000;//判定する時間、デフォルトは五秒間分の値を使う
	public static final String	TIME_DIFFERENCE_THRESHOLD			= "TIME_DIFFERENCE_THRESHOLD";
	public static final long	TIME_DIFFERENCE_THRESHOLD_DEFAULT	= 1000;//msec、加速度と角度のセンサの時間が離れすぎてないことを祈る、祈ってたらこいつ必要ないことに気づいた
	public static final String	NUMBER_OF_STEPS						= "NUMBER_OF_STEPS";
	public static final String	WALK_COUNT							= "WALK_COUNT";//万歩計ってPedometerだったり、歩数だとNumber of stepsだったり。うーむ walk countってだめ？
	public static final String	WALK_COUNT_DAY						= "WALK_COUNT_DAY";
	public static final String	WALK_COUNT_THRESHOLD 				= "WALK_COUNT_THRESHOLD";
	public static final float	WALK_COUNT_THRESHOLD_DEFAULT 		= 10.4F;//加速度, 5.4F
	public static final String	WALK_THRESHOLD 						= "WALK_THRESHOLD";
	public static final int		WALK_THRESHOLD_DEFAULT 				= 4;//歩数
	public static final String	STAIR_THRESHOLD						= "STAIR_THRESHOLD";
	public static final int		STAIR_THRESHOLD_DEFAULT				= 800;//分散、あとで閾値調整なぁー
	public static final String	ELEVATOR_THRESHOLD					= "ELEVATOR_THRESHOLD";
	public static final long	ELEVATOR_THRESHOLD_DEFAULT			= 1900;//msec、
	public static final String	ELEVATOR_DIFFERENT_INTERVAL			= "ELEVATOR_DIFFERENT_INTERVAL_THRESHOLD";
	public static final int		ELEVATOR_DIFFERENT_INTERVAL_DEFAULT	= 10;//微分する際の間隔
	public static final String	ELEVATOR_DIFFERENT_RANGE			= "ELEVATOR_DIFFERENT_RANGE";
	public static final float	ELEVATOR_DIFFERENT_RANGE_DEFAULT	= 0.8F;//どこまでの誤差を許容するか
	public static final String	ELEVATOR_USE_IN						= "ELEVATOR_USE_IN";
	public static final boolean	ELEVATOR_USE_IN_DEFAULT				= true;//判定時間が短い場合エレベータの開始と終了を同時に取得できないため、現在エレベータの中にいるかどうかを判定する
	public static final String	ELEVATOR_FEATURE_TIMES				= "ELEVATOR_FEATURE_TIMES";
	public static final int		ELEVATOR_FEATURE_TIMES_DEFAULT		= 1;//何回エレベータの特徴を抽出できたか

	public StateInference(Context context) {
		// ⊂(^ω^ )二二⊃こいつぁ間に合いそうにないお 2013/03/10
		// ⊂(^ω^ )二二⊃こんかいも間に合いそうにないお 2013/03/18
		// ⊂(^ω^ )二二⊃やっぱむりだお 2013/03/21
		// ⊂(;ω; )二二⊃ひぎぃあふぅぅぅぅぅぅ 2013/03/24
		// ⊂(｀・ω・´)二二⊃むりだったお 2013/03/25
		mContext = context;
		sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		numSteps = sp.getInt(NUMBER_OF_STEPS, 0);
		dayWalk = sp.getLong(WALK_COUNT_DAY, getDayTime());
		stackStep = 0;

		// センサー設定
		stateLog = new StateLog(StateLog.STATE_STOP);
		inElevator = false;

		//デバッグログ
		debugId = 0;
		debugLog = "";
		fileExist = true;
		File file = new File(Environment.getExternalStorageDirectory() + FILE_DIRECTORY);
		if(!file.exists()){
			if(!file.mkdir())	fileExist = false;
		}
		if(fileExist){
			file = new File(Environment.getExternalStorageDirectory() + FILE_DIRECTORY + FILE_NAME);
			if(!file.exists()){
				try{if(!file.createNewFile())	fileExist = false;}
				catch(Exception e){fileExist = false;}
			}
		}
	}

	public void stop() {

		sp.edit().putInt(NUMBER_OF_STEPS, numSteps).commit();
	}

	public void inference(List<SensorData> acceles, List<SensorData> orientations) {//インファレンスですの
		Log.e("データ数","[Accele:"+acceles.size()+"]");
		Log.e("データ数","[Orient:"+orientations.size()+"]");
		if(acceles.size() <= sp.getInt(ELEVATOR_DIFFERENT_INTERVAL, ELEVATOR_DIFFERENT_INTERVAL_DEFAULT) || orientations.size() <= 0)	return;//値がなければ計算できませんよ
		//平均とか分散とか垂直加速度とか歩数とか
		ArrayList<Float> verticalAcceles = calcVerticalAcceleration(acceles, orientations);
		int walkCount = newCountWalk(verticalAcceles);
		numSteps += walkCount;
		stackStep += walkCount;
		overTheDay();
		sp.edit().putInt(NUMBER_OF_STEPS, numSteps).commit();

		judge(walkCount, acceles, orientations, verticalAcceles);
	}

	public void judge(int walkCount, List<SensorData> acceles, List<SensorData> orientations, ArrayList<Float> verticalAcceles){//ジャッジメントですの
		//判定するよー！
		//歩行かそうでないかを判定した後、歩行の場合階段かどうか、歩行でないばあいエレベータの判定を行う
		debugLog += walkCount + ",";
		if(judgeWalk(walkCount)){
			inElevator = false;// エレベータ内では歩かないようお願い申し上げます
			float[] orientationXZVariance = calcOrientationXZVariance(orientations);
			debugLog +=  orientationXZVariance[0]+ "," + orientationXZVariance[1] + ",";
			if(judgeStair(orientationXZVariance[0], orientationXZVariance[1])){
				stateLog.setStair(acceles.get(acceles.size() - 1).timestamp);
				debugLog += ",,,," + StateLog.STATE_STAIR;
			}
			else{
				stateLog.setWalk(acceles.get(acceles.size() - 1).timestamp);
				debugLog += ",,,," + StateLog.STATE_WALK;
			}

		}
		else{
			/*
			debugLog +=  ",," + ((inElevator)?1:0);
			if(judgeElevator(verticalAcceles, acceles, inElevator)){
				inElevator = true;// エレベータ内である
				stateLog.setElevator(acceles.get(acceles.size() - 1).timestamp);
				debugLog += "," + StateLog.STATE_ELEVATOR;
			} else {
				stateLog.setStop(acceles.get(acceles.size() - 1).timestamp);
				debugLog += "," + StateLog.STATE_STOP;
			}
			*/
			stateLog.setStop(acceles.get(acceles.size() - 1).timestamp);
			debugLog += ",,,,,," + StateLog.STATE_STOP;
		}

		SimpleDateFormat sdf = new SimpleDateFormat(CloudManager.DATETIME_PATTERN, Locale.JAPAN);
		saveDebugLog(FILE_NAME, debugLog + "," + sdf.format(new Date()), debugId);
		debugLog = "";
		debugId++;
	}

	public boolean judgeWalk(int walkCount) {
		if (walkCount >= sp.getInt(WALK_THRESHOLD, WALK_THRESHOLD_DEFAULT)) {
			return true;
		}
		else	return false;
	}
	public boolean judgeStair(float orientationXVariance, float orientationZVariance){
		//あとでSkewness使った判定方式に変更する、きっとする
		if ((orientationXVariance + orientationZVariance) < sp.getInt(STAIR_THRESHOLD, STAIR_THRESHOLD_DEFAULT)) {
			return true;
		}
		else	return false;
	}
	public boolean judgeElevator(ArrayList<Float> verticalAcceles, List<SensorData> acceles, boolean inElevator){
		//timestampを持ってるのは元データだから引数にとってる。verticalAccelesをSensorData型にすればいいんだけど、無駄なデータを持たせたくない…
		ArrayList<Float> differentVerticalAcceles = new ArrayList<Float>();
		int differentInterval = sp.getInt(ELEVATOR_DIFFERENT_INTERVAL, ELEVATOR_DIFFERENT_INTERVAL_DEFAULT);
		float differentRange = sp.getFloat(ELEVATOR_DIFFERENT_RANGE, ELEVATOR_DIFFERENT_RANGE_DEFAULT);
		long indexDistinctThreshold = sp.getLong(ELEVATOR_THRESHOLD, ELEVATOR_THRESHOLD_DEFAULT);
		int judgeElevator = 0;

		//微分を行いつつ、最大値と最小値も求める
		differentVerticalAcceles.add(verticalAcceles.get(differentInterval) - verticalAcceles.get(0));
		float differentVerticalAcceleMax = differentVerticalAcceles.get(0);
		float differentVerticalAcceleMin = differentVerticalAcceles.get(0);
		for (int i = differentInterval + 1; i < verticalAcceles.size(); i++) {
			differentVerticalAcceles.add(verticalAcceles.get(i) - verticalAcceles.get(i - differentInterval));
			if(differentVerticalAcceleMax < differentVerticalAcceles.get(i - differentInterval))
				differentVerticalAcceleMax = differentVerticalAcceles.get(i - differentInterval);
			else if(differentVerticalAcceleMin > differentVerticalAcceles.get(i - differentInterval))
				differentVerticalAcceleMin = differentVerticalAcceles.get(i - differentInterval);
		}

		debugLog += "," + differentVerticalAcceleMax + "," + differentVerticalAcceleMin;

		// エレベータ判定
		//まずエレベータが上昇しているのか下降しているのか考える、この判定方式の残念さは、きっと未来の誰かが何とかするさ
		int featureIndex = differentVerticalAcceles.size();//特徴のインデックス
		boolean upElevator = true;//エレベータが下降するとき垂直加速度は増加する、逆もまたしかりなかんじで
		for(int i = 0;i < differentVerticalAcceles.size();i++){
			if (differentVerticalAcceles.get(i) > differentVerticalAcceleMax * differentRange) {
				featureIndex = i;
				upElevator = false;
				break;
			} else if (differentVerticalAcceles.get(i) < differentVerticalAcceleMin * differentRange) {
				featureIndex = i;
				upElevator = true;
				break;
			}
		}
		//debugLog += "," + featureIndex + "," + upElevator;
		debugLog += "," + ((upElevator)?1:0);
		for(int i = featureIndex;i < differentVerticalAcceles.size();i++){
			if(upElevator){
				if (differentVerticalAcceles.get(i) > differentVerticalAcceleMax * differentRange) {
					if(acceles.get(i + differentInterval).timestamp -
							acceles.get(featureIndex + differentInterval).timestamp >=
							indexDistinctThreshold)
						judgeElevator++;//これバグかな、たぶん
					featureIndex = i;
					upElevator = false;
				}
			}
			else{
				if (differentVerticalAcceles.get(i) < differentVerticalAcceleMin * differentRange) {
					if(acceles.get(i + differentInterval).timestamp -
							acceles.get(featureIndex + differentInterval).timestamp >=
							indexDistinctThreshold)
						judgeElevator++;
					featureIndex = i;
					upElevator = true;
				}
			}
		}
		//debugLog += "," + judgeElevator;
		if (inElevator && sp.getBoolean(ELEVATOR_USE_IN, ELEVATOR_USE_IN_DEFAULT) ||
				judgeElevator >= sp.getInt(ELEVATOR_FEATURE_TIMES, ELEVATOR_FEATURE_TIMES_DEFAULT)) {
				if(inElevator) inElevator = false;
			return true;
		}
		else return false;
	}

	//基準から、超えている間と未満の間、それぞれの最大最小値を比較
	//基準から、超えている間と未満の間、それぞれの平均値を比較m 
	//重力と、平均値それぞれを基準に使った方法
	//合成加速度、垂直加速度それぞれを使った方法
	private float NEW_WALK_COUNT_FIRST_THRESHOLD = 0;
	private float NEW_WALK_COUNT_SECOND_ACCELE_THRESHOLD = 0.95f;
	public int newCountWalk(ArrayList<Float> verticalAcceles) {
		//9.8を前後するかどうか
		int count = 0;
		float acceleHigh = verticalAcceles.get(0);
		float acceleLow = verticalAcceles.get(0);
		boolean upStep = true;
		float avg = 0;

		for(float accele : verticalAcceles){
			if(upStep){
				if(accele > acceleHigh)	acceleHigh = accele;
				if(accele >= SensorManager.GRAVITY_EARTH + this.NEW_WALK_COUNT_FIRST_THRESHOLD){
					upStep = false;
					acceleLow = accele;
				}
			}else{
				if(accele < acceleLow)	acceleLow = accele;
				if(accele <= SensorManager.GRAVITY_EARTH + this.NEW_WALK_COUNT_FIRST_THRESHOLD){
					upStep = true;
					//Log.e("Walk",acceleHigh - acceleLow + "");
					avg += acceleHigh - acceleLow;
					if(acceleHigh - acceleLow > NEW_WALK_COUNT_SECOND_ACCELE_THRESHOLD){
						count++;
					}
					acceleHigh = accele;
				}
			}
		}
		//Toast.makeText(mContext.getApplicationContext(), avg + "", Toast.LENGTH_SHORT).show();
		return count;
	}
	public int countWalk(ArrayList<Float> verticalAcceles) {
		int walkCount = 0;
		boolean upWalk = true;//加速度が上昇しているか
		float acceleHigh = verticalAcceles.get(0);
		float acceleLow = verticalAcceles.get(0);
		int i = 0;

		//加速度の増加減少の勢いを見て、閾値を超えた場合一歩とカウントする
		for(float accele : verticalAcceles){
			if (upWalk) {//加速度が上昇時にHighを更新、下降時にLowを更新して下降判定に移る
				if (acceleHigh <= accele) acceleHigh = accele;
				else {
					//debugLog += "up," + i + "," + acceleHigh + "," + acceleLow + "," + (acceleHigh - acceleLow) + "\n";
					//if (acceleHigh - acceleLow > sp.getFloat(WALK_COUNT_THRESHOLD, WALK_COUNT_THRESHOLD_DEFAULT))	walkCount++;
					acceleLow = accele;
					upWalk = false;
				}
			} else {// 下降時も似た感じで
				//debugLog += "down," + i + "," + acceleHigh + "," + acceleLow + "," + (acceleHigh - acceleLow) + "\n";
				if (acceleLow >= accele) acceleLow = accele;
				else {
					if (acceleHigh - acceleLow > sp.getFloat(WALK_COUNT_THRESHOLD, WALK_COUNT_THRESHOLD_DEFAULT))	walkCount++;
					acceleHigh = accele;
					upWalk = true;
				}
			}
			i++;
		}
		return walkCount;
	}

	public float[] calcOrientationXZVariance(List<SensorData> datas){
		//計算量減らすために汎用性を犠牲にした。もっといい方法があるはずだし、使いまわせない関数って嫌いなので、任せたぜ未来のオレ
		float avg1 = 0, avg2 = 0;
		float variance1 = 0, variance2 = 0;
		for(SensorData data : datas){
			//Log.e("inference","[X:" + Math.toDegrees(data.values[1]) + "]");
			avg1 += Math.toDegrees(data.values[1]);
			avg2 += Math.toDegrees(data.values[2]);
			variance1 += Math.toDegrees(data.values[1]) * Math.toDegrees(data.values[1]);
			variance2 += Math.toDegrees(data.values[2]) * Math.toDegrees(data.values[2]);
		}
		avg1 = avg1 / datas.size();
		avg2 = avg2 / datas.size();
		variance1 = (variance1 / datas.size()) - (avg1 * avg1);
		variance2 = (variance2 / datas.size()) - (avg2 * avg2);
		return new float[]{variance1, variance2};
	}
	public float[] calcAverageAndVariance(float[] datas){
		float avg = 0;
		float variance = 0;
		for(float data : datas){
			avg += data;
			variance += data * data;
		}
		avg = avg / datas.length;
		variance = (variance / datas.length) - (avg * avg);
		return new float[]{avg, variance};
	}

	public ArrayList<Float> calcVerticalAcceleration(List<SensorData> accele,List<SensorData> orientation){// 垂直加速度の配列の計算
		ArrayList<Float> verticalAcceles = new ArrayList<Float>();
		int orientationIndex = 0;
		boolean checkIndex = true;
		try {
		for(int i = 0;i < accele.size();i++){
			while(checkIndex){//加速度の取得時と回転度の取得時がずれすぎていないかをチェック、もっといい方法あるよなぁ、そのうち改善する…
				if(Math.abs(orientation.get(orientationIndex).timestamp - accele.get(i).timestamp) <
						Math.abs(orientation.get(orientationIndex+1).timestamp - accele.get(i).timestamp)){
					break;
				}
				orientationIndex++;
				if(orientationIndex >= (orientation.size() - 1)){
					if(Math.abs(orientation.get(orientationIndex).timestamp - accele.get(i).timestamp) >
					Math.abs(orientation.get(orientationIndex-1).timestamp - accele.get(i).timestamp)){
						orientationIndex--;
					}
					checkIndex = false;
				}
			}
			verticalAcceles.add(calcVerticalAcceleration(accele.get(i), orientation.get(orientationIndex)));
		}
		} catch(Exception e){}
		return verticalAcceles;
	}
	public float calcVerticalAcceleration(SensorData accele,SensorData orientation){// 垂直加速度のSensorData型からfloatにして計算
		return calcVerticalAcceleration(accele.values, orientation.values);
	}
	public float calcVerticalAcceleration(float[] accele,float[] orientation){// 垂直加速度の計算
		//加速度三軸をXYZの順で、角度はYXZの順かつラジアンでおなしゃーす
		float verticalAccele = (float)(
				accele[0] * Math.cos(orientation[1]) * Math.sin(orientation[2])
				- accele[1] * Math.sin(orientation[1]) * Math.cos(orientation[2])
				+ accele[2] * Math.cos(orientation[1]) * Math.cos(orientation[2]));
		return verticalAccele;
	}

	public static void saveDebugLog(String fileName, String logs, int id){
		logs = logs + "\n";
		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(Environment.getExternalStorageDirectory() +
					FILE_DIRECTORY +
					fileName,true);
			fos.write(logs.toString().getBytes(), 0, logs.length());
			fos.flush();
		}
		catch (Exception e) {e.printStackTrace();Log.e("inferenceFile","exception1");}
		finally {
			try {
				if( fos != null ){
					fos.close();
				}
			}
			catch( IOException e ){e.printStackTrace();Log.e("inferenceFile","exception1");}
		}
	}

	public void overTheDay(){//今日を越えて
		if(java.lang.System.currentTimeMillis() >= dayWalk + (24 * 60 * 60 * 1000)){
			//DayLogDBManager.insertStepLog(mContext, numSteps, dayWalk);
			numSteps = 0;
			dayWalk = getDayTime();
			sp.edit().putLong(WALK_COUNT_DAY, dayWalk).commit();
			}
	}
	public long getDayTime(){
		Calendar calendar = Calendar.getInstance();
		calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
		calendar.add(Calendar.MILLISECOND, -calendar.get(Calendar.MILLISECOND));
		return calendar.getTimeInMillis();
	}
}
