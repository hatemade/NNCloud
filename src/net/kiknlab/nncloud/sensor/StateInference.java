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

public class StateInference {//��Ԑ���
	SharedPreferences sp;
	Context mContext;
	// ����ϐ�
	public StateLog stateLog;
	private boolean inElevator;
	public int numSteps;//�Ƃ肠��������̕������ق�
	public int stackStep;//���}���̕���
	public long dayWalk;

	public String debugLog;
	public boolean fileExist;
	public int debugId;
	public static final String FILE_NAME= "inference_log.csv";
	public static final String FILE_DIRECTORY = "/NNCloud/";

	// �X���b�h�O�����Ă����
	//final int INTERVAL_PERIOD = 20000;// msec���Ǝv�������C�����肾��I������{�Imilliseconds�ł��c�͂��c
	//Timer timer = new Timer();

	//�ォ��ǉ������̂ŁA���Ƃ��琮������
	//臒l�͎��Ԃ����Ƃɑ������������Ȃ�A�X�̃f�t�H���g�Ɏ��Ԃ�������΂������Ȃ�
	public static final String	TIME_LENGTH							= "TIME_LENGTH";
	public static final long	TIME_LENGTH_DEFAULT					= 5000;//���肷�鎞�ԁA�f�t�H���g�͌ܕb�ԕ��̒l���g��
	public static final String	TIME_DIFFERENCE_THRESHOLD			= "TIME_DIFFERENCE_THRESHOLD";
	public static final long	TIME_DIFFERENCE_THRESHOLD_DEFAULT	= 1000;//msec�A�����x�Ɗp�x�̃Z���T�̎��Ԃ����ꂷ���ĂȂ����Ƃ��F��A�F���Ă��炱���K�v�Ȃ����ƂɋC�Â���
	public static final String	NUMBER_OF_STEPS						= "NUMBER_OF_STEPS";
	public static final String	WALK_COUNT							= "WALK_COUNT";//�����v����Pedometer��������A��������Number of steps��������B���[�� walk count���Ă��߁H
	public static final String	WALK_COUNT_DAY						= "WALK_COUNT_DAY";
	public static final String	WALK_COUNT_THRESHOLD 				= "WALK_COUNT_THRESHOLD";
	public static final float	WALK_COUNT_THRESHOLD_DEFAULT 		= 10.4F;//�����x, 5.4F
	public static final String	WALK_THRESHOLD 						= "WALK_THRESHOLD";
	public static final int		WALK_THRESHOLD_DEFAULT 				= 4;//����
	public static final String	STAIR_THRESHOLD						= "STAIR_THRESHOLD";
	public static final int		STAIR_THRESHOLD_DEFAULT				= 800;//���U�A���Ƃ�臒l�����Ȃ��[
	public static final String	ELEVATOR_THRESHOLD					= "ELEVATOR_THRESHOLD";
	public static final long	ELEVATOR_THRESHOLD_DEFAULT			= 1900;//msec�A
	public static final String	ELEVATOR_DIFFERENT_INTERVAL			= "ELEVATOR_DIFFERENT_INTERVAL_THRESHOLD";
	public static final int		ELEVATOR_DIFFERENT_INTERVAL_DEFAULT	= 10;//��������ۂ̊Ԋu
	public static final String	ELEVATOR_DIFFERENT_RANGE			= "ELEVATOR_DIFFERENT_RANGE";
	public static final float	ELEVATOR_DIFFERENT_RANGE_DEFAULT	= 0.8F;//�ǂ��܂ł̌덷�����e���邩
	public static final String	ELEVATOR_USE_IN						= "ELEVATOR_USE_IN";
	public static final boolean	ELEVATOR_USE_IN_DEFAULT				= true;//���莞�Ԃ��Z���ꍇ�G���x�[�^�̊J�n�ƏI���𓯎��Ɏ擾�ł��Ȃ����߁A���݃G���x�[�^�̒��ɂ��邩�ǂ����𔻒肷��
	public static final String	ELEVATOR_FEATURE_TIMES				= "ELEVATOR_FEATURE_TIMES";
	public static final int		ELEVATOR_FEATURE_TIMES_DEFAULT		= 1;//����G���x�[�^�̓����𒊏o�ł�����

	public StateInference(Context context) {
		// ��(^��^ )��񁽂������Ԃɍ��������ɂȂ��� 2013/03/10
		// ��(^��^ )��񁽂��񂩂����Ԃɍ��������ɂȂ��� 2013/03/18
		// ��(^��^ )��񁽂���ςނ肾�� 2013/03/21
		// ��(;��; )��񁽂Ђ������ӂ����������� 2013/03/24
		// ��(�M�E�ցE�L)��񁽂ނ肾������ 2013/03/25
		mContext = context;
		sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		numSteps = sp.getInt(NUMBER_OF_STEPS, 0);
		dayWalk = sp.getLong(WALK_COUNT_DAY, getDayTime());
		stackStep = 0;

		// �Z���T�[�ݒ�
		stateLog = new StateLog(StateLog.STATE_STOP);
		inElevator = false;

		//�f�o�b�O���O
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

	public void inference(List<SensorData> acceles, List<SensorData> orientations) {//�C���t�@�����X�ł���
		Log.e("�f�[�^��","[Accele:"+acceles.size()+"]");
		Log.e("�f�[�^��","[Orient:"+orientations.size()+"]");
		if(acceles.size() <= sp.getInt(ELEVATOR_DIFFERENT_INTERVAL, ELEVATOR_DIFFERENT_INTERVAL_DEFAULT) || orientations.size() <= 0)	return;//�l���Ȃ���Όv�Z�ł��܂����
		//���ςƂ����U�Ƃ����������x�Ƃ������Ƃ�
		ArrayList<Float> verticalAcceles = calcVerticalAcceleration(acceles, orientations);
		int walkCount = newCountWalk(verticalAcceles);
		numSteps += walkCount;
		stackStep += walkCount;
		overTheDay();
		sp.edit().putInt(NUMBER_OF_STEPS, numSteps).commit();

		judge(walkCount, acceles, orientations, verticalAcceles);
	}

	public void judge(int walkCount, List<SensorData> acceles, List<SensorData> orientations, ArrayList<Float> verticalAcceles){//�W���b�W�����g�ł���
		//���肷���[�I
		//���s�������łȂ����𔻒肵����A���s�̏ꍇ�K�i���ǂ����A���s�łȂ��΂����G���x�[�^�̔�����s��
		debugLog += walkCount + ",";
		if(judgeWalk(walkCount)){
			inElevator = false;// �G���x�[�^���ł͕����Ȃ��悤���肢�\���グ�܂�
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
				inElevator = true;// �G���x�[�^���ł���
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
		//���Ƃ�Skewness�g������������ɕύX����A�����Ƃ���
		if ((orientationXVariance + orientationZVariance) < sp.getInt(STAIR_THRESHOLD, STAIR_THRESHOLD_DEFAULT)) {
			return true;
		}
		else	return false;
	}
	public boolean judgeElevator(ArrayList<Float> verticalAcceles, List<SensorData> acceles, boolean inElevator){
		//timestamp�������Ă�̂͌��f�[�^����������ɂƂ��Ă�BverticalAcceles��SensorData�^�ɂ���΂����񂾂��ǁA���ʂȃf�[�^�������������Ȃ��c
		ArrayList<Float> differentVerticalAcceles = new ArrayList<Float>();
		int differentInterval = sp.getInt(ELEVATOR_DIFFERENT_INTERVAL, ELEVATOR_DIFFERENT_INTERVAL_DEFAULT);
		float differentRange = sp.getFloat(ELEVATOR_DIFFERENT_RANGE, ELEVATOR_DIFFERENT_RANGE_DEFAULT);
		long indexDistinctThreshold = sp.getLong(ELEVATOR_THRESHOLD, ELEVATOR_THRESHOLD_DEFAULT);
		int judgeElevator = 0;

		//�������s���A�ő�l�ƍŏ��l�����߂�
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

		// �G���x�[�^����
		//�܂��G���x�[�^���㏸���Ă���̂����~���Ă���̂��l����A���̔�������̎c�O���́A�����Ɩ����̒N�������Ƃ����邳
		int featureIndex = differentVerticalAcceles.size();//�����̃C���f�b�N�X
		boolean upElevator = true;//�G���x�[�^�����~����Ƃ����������x�͑�������A�t���܂�������Ȃ��񂶂�
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
						judgeElevator++;//����o�O���ȁA���Ԃ�
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

	//�����A�����Ă���ԂƖ����̊ԁA���ꂼ��̍ő�ŏ��l���r
	//�����A�����Ă���ԂƖ����̊ԁA���ꂼ��̕��ϒl���rm 
	//�d�͂ƁA���ϒl���ꂼ�����Ɏg�������@
	//���������x�A���������x���ꂼ����g�������@
	private float NEW_WALK_COUNT_FIRST_THRESHOLD = 0;
	private float NEW_WALK_COUNT_SECOND_ACCELE_THRESHOLD = 0.95f;
	public int newCountWalk(ArrayList<Float> verticalAcceles) {
		//9.8��O�シ�邩�ǂ���
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
		boolean upWalk = true;//�����x���㏸���Ă��邩
		float acceleHigh = verticalAcceles.get(0);
		float acceleLow = verticalAcceles.get(0);
		int i = 0;

		//�����x�̑��������̐��������āA臒l�𒴂����ꍇ����ƃJ�E���g����
		for(float accele : verticalAcceles){
			if (upWalk) {//�����x���㏸����High���X�V�A���~����Low���X�V���ĉ��~����Ɉڂ�
				if (acceleHigh <= accele) acceleHigh = accele;
				else {
					//debugLog += "up," + i + "," + acceleHigh + "," + acceleLow + "," + (acceleHigh - acceleLow) + "\n";
					//if (acceleHigh - acceleLow > sp.getFloat(WALK_COUNT_THRESHOLD, WALK_COUNT_THRESHOLD_DEFAULT))	walkCount++;
					acceleLow = accele;
					upWalk = false;
				}
			} else {// ���~��������������
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
		//�v�Z�ʌ��炷���߂ɔėp�����]���ɂ����B�����Ƃ������@������͂������A�g���܂킹�Ȃ��֐����Č����Ȃ̂ŁA�C�����������̃I��
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

	public ArrayList<Float> calcVerticalAcceleration(List<SensorData> accele,List<SensorData> orientation){// ���������x�̔z��̌v�Z
		ArrayList<Float> verticalAcceles = new ArrayList<Float>();
		int orientationIndex = 0;
		boolean checkIndex = true;
		try {
		for(int i = 0;i < accele.size();i++){
			while(checkIndex){//�����x�̎擾���Ɖ�]�x�̎擾�������ꂷ���Ă��Ȃ������`�F�b�N�A�����Ƃ������@�����Ȃ��A���̂������P����c
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
	public float calcVerticalAcceleration(SensorData accele,SensorData orientation){// ���������x��SensorData�^����float�ɂ��Čv�Z
		return calcVerticalAcceleration(accele.values, orientation.values);
	}
	public float calcVerticalAcceleration(float[] accele,float[] orientation){// ���������x�̌v�Z
		//�����x�O����XYZ�̏��ŁA�p�x��YXZ�̏������W�A���ł��Ȃ���[��
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

	public void overTheDay(){//�������z����
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
