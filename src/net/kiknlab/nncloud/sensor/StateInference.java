package net.kiknlab.nncloud.sensor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import net.kiknlab.nncloud.db.LearningDBManager;
import net.kiknlab.nncloud.util.SensorData;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.preference.PreferenceManager;
import android.util.Log;

public class StateInference {//状態推定
	SharedPreferences sp;
	Context mContext;
	public static final String MILEAGE_POINT = "MILAGE_POINT";
	public float mile;
	// 推定変数
	public String state;
	public int numSteps;
	private boolean inElevator;

	// スレッド三回回ってわんわん
	final int INTERVAL_PERIOD = 20000;// msecかと思ったか，あたりだよ！もう一本！millisecondsです…はい…
	Timer timer = new Timer();

	//後から追加したので、あとから整理する
	//閾値は時間をもとに増減させたいなら、個々のデフォルトに時間をかければいいかなぁ
	public static final String	TIME_LENGTH							= "TIME_LENGTH";
	public static final long	TIME_LENGTH_DEFAULT					= 5000;//判定する時間、デフォルトは五秒間分の値を使う
	public static final String	TIME_DIFFERENCE_THRESHOLD			= "TIME_DIFFERENCE_THRESHOLD";
	public static final long	TIME_DIFFERENCE_THRESHOLD_DEFAULT	= 1000;//msec、加速度と角度のセンサの時間が離れすぎてないことを祈る、祈ってたらこいつ必要ないことに気づいた
	public static final String	WALK_COUNT							= "WALK_COUNT";//万歩計ってPedometerだったり、歩数だとNumber of stepだったり。うーむ walk countってだめ？
	public static final String	WALK_COUNT_THRESHOLD 				= "WALK_COUNT_THRESHOLD";
	public static final float	WALK_COUNT_THRESHOLD_DEFAULT 		= 5.4F;//加速度
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
	public static final float	POWER_SAVING_STAIR					= 0.74f;//階段利用時の節電量。単位は…？なんだっけ?ワット?
	public static final float	POWER_USING_ELEVATOR				= -3.33f;//エレベータ利用時の消費電力

	public StateInference(Context context) {
		// ⊂(^ω^ )二二⊃こいつぁ間に合いそうにないお 2013/03/10
		// ⊂(^ω^ )二二⊃こんかいも間に合いそうにないお 2013/03/18
		mContext = context;
		sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		mile = sp.getFloat(MILEAGE_POINT, 0);

		// センサー設定
		state = "stop";
		inElevator = false;

		//すれっどすれっどたのしいな
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				Log.e("遅い","0");
				inference();// 推定
				Log.e("遅い","11");
			}
		}, 0, INTERVAL_PERIOD);
	}

	public void stop() {
		// 解放せよ！われらは働かない者である！
		sp.edit().putFloat(MILEAGE_POINT, mile).commit();
		timer.cancel();
		timer = null;
		// 解放された―、うまうま
	}

	public void inference() {//インファレンスですの
		//推定に必要な変数
		long judgeTime = sp.getLong(TIME_LENGTH, TIME_LENGTH_DEFAULT);//判定時間を決めます
		//センサの値
		long time = java.lang.System.currentTimeMillis();
		ArrayList<SensorData> acceles = LearningDBManager.getSensorData(
				mContext, Sensor.TYPE_ACCELEROMETER, time, judgeTime);
		Log.e("遅い","1.2");
		ArrayList<SensorData> orientations = LearningDBManager.getSensorData(
				mContext, SensorAdmin.TYPE_ORIENTATION_MAKE, time, judgeTime);
		Log.e("遅い","2");
		SimpleDateFormat sdf = new SimpleDateFormat("HH':'mm':'ss'.'SSS");
		Log.e("データ数","[Accele:"+acceles.size()+"]");
		Log.e("データ数","[Orient:"+orientations.size()+"]");
		Log.e("現在時",sdf.format(new Date(time)) + "");
		for(int i = 0;i < acceles.size();i++){
			Log.e("Acceles","["+i+"]["+sdf.format(new Date(acceles.get(i).timestamp))+"]" + acceles.get(i).timestamp);
		}
		if(acceles.size() <= sp.getInt(ELEVATOR_DIFFERENT_INTERVAL, ELEVATOR_DIFFERENT_INTERVAL_DEFAULT) || orientations.size() <= 0)	return;//値がなければ計算できませんよ
		Log.e("遅い","3");
		//平均とか分散とか垂直加速度とか歩数とか、多分この四つで全部？二つで済んだ
		ArrayList<Float> verticalAcceles = calcVerticalAcceleration(acceles, orientations);
		Log.e("遅い","4");
		int walkCount = 0;//countWalk(verticalAcceles);
		Log.e("遅い","5");
		this.numSteps += walkCount;
		Log.e("遅い","6");
	}
	
	public void judge(int walkCount, ArrayList<SensorData> acceles, ArrayList<SensorData> orientations, ArrayList<Float> verticalAcceles){//ジャッジメントですの
		//判定するよー！
		// 歩行かそうでないかを判定した後、歩行の場合階段かどうか、歩行でないばあいエレベータの判定を行う
		if(judgeWalk(walkCount)){
			inElevator = false;// エレベータ内では歩かないようお願い申し上げます
			float[] orientationXZVariance = calcOrientationXZVariance(orientations);
			if(judgeStair(orientationXZVariance[0], orientationXZVariance[1])){
				mile += POWER_SAVING_STAIR;//あとで増える量を経過時間をみて変わるように調整する
				state = "stair";
			}
			else	state = "walk";
		}
		else{
			Log.e("遅い","7");
			if(judgeElevator(verticalAcceles, acceles, inElevator)){
				Log.e("遅い","8");
				inElevator = true;// エレベータ内である
				mile += POWER_USING_ELEVATOR;
				state = "elevator";// エレベーター判定
				Log.e("遅い","9");
			} else {
				state = "stop";
			}
		}
		Log.e("遅い","10");
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
	public boolean judgeElevator(ArrayList<Float> verticalAcceles, ArrayList<SensorData> acceles, boolean inElevator){
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

		// エレベータ判定
		//まずエレベータが上昇しているのか下降しているのか考える、この判定方式の残念さは、きっと未来の誰かが何とかするさ
		int featureIndex = 0;//特徴のインデックス
		boolean upElevator = true;//エレベータが下降するとき垂直加速度は増加する、逆もまたしかりなかんじで
		for(int i = 0;i < differentVerticalAcceles.size();i++){
			if (differentVerticalAcceles.get(i) > differentVerticalAcceleMax * differentRange) {
				featureIndex = i;
				upElevator = false;
			} else if (differentVerticalAcceles.get(i) < differentVerticalAcceleMin * differentRange) {
				featureIndex = i;
				upElevator = true;
			}
		}
		for(int i = featureIndex;i < differentVerticalAcceles.size();i++){
			if(upElevator){
				if (differentVerticalAcceles.get(i) > differentVerticalAcceleMax * differentRange) {
					if(acceles.get(i + differentInterval).timestamp -
							acceles.get(featureIndex + differentInterval).timestamp >=
							indexDistinctThreshold)
						judgeElevator++;
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
		if (inElevator && sp.getBoolean(ELEVATOR_USE_IN, ELEVATOR_USE_IN_DEFAULT) ||
				judgeElevator >= sp.getInt(ELEVATOR_FEATURE_TIMES, ELEVATOR_FEATURE_TIMES_DEFAULT)) {
			return true;
		}
		else return false;
	}

	public int countWalk(ArrayList<Float> verticalAcceles) {
		int walkCount = 0;
		boolean upWalk = true;//加速度が上昇しているか
		float acceleHigh = verticalAcceles.get(0);
		float acceleLow = verticalAcceles.get(0);

		//加速度の増加減少の勢いを見て、閾値を超えた場合一歩とカウントする
		for(float accele : verticalAcceles){
			if (upWalk) {//加速度が上昇時にHighを更新、下降時にLowを更新して下降判定に移る
				if (acceleHigh <= accele) acceleHigh = accele;
				else {
					if (acceleHigh - acceleLow > sp.getFloat(WALK_COUNT_THRESHOLD, WALK_COUNT_THRESHOLD_DEFAULT))	walkCount++;
					acceleLow = accele;
					upWalk = false;
				}
			} else {// 下降時も似た感じで
				if (acceleLow >= accele) acceleLow = accele;
				else {
					if (acceleHigh - acceleLow > sp.getFloat(WALK_COUNT_THRESHOLD, WALK_COUNT_THRESHOLD_DEFAULT))	walkCount++;
					acceleHigh = accele;
					upWalk = true;
				}
			}
		}
		return walkCount;
	}

	public float[] calcOrientationXZVariance(ArrayList<SensorData> datas){
		//計算量減らすために汎用性を犠牲にした。もっといい方法があるはずだし、使いまわせない関数って嫌いなので、任せたぜ未来のオレ
		float avg1 = 0, avg2 = 0;
		float variance1 = 0, variance2 = 0;
		for(SensorData data : datas){
			avg1 += data.values[1];
			avg2 += data.values[2];
			variance1 += data.values[1] * data.values[1];
			variance2 += data.values[2] * data.values[2];
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

	public ArrayList<Float> calcVerticalAcceleration(ArrayList<SensorData> accele,ArrayList<SensorData> orientation){// 垂直加速度の配列の計算
		ArrayList<Float> verticalAcceles = new ArrayList<Float>();
		int orientationIndex = 0;
		boolean checkIndex = true;
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
}
