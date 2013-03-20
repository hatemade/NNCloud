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

public class StateInference {//��Ԑ���
	SharedPreferences sp;
	Context mContext;
	public static final String MILEAGE_POINT = "MILAGE_POINT";
	public float mile;
	// ����ϐ�
	public String state;
	public int numSteps;
	private boolean inElevator;

	// �X���b�h�O�����Ă����
	final int INTERVAL_PERIOD = 20000;// msec���Ǝv�������C�����肾��I������{�Imilliseconds�ł��c�͂��c
	Timer timer = new Timer();

	//�ォ��ǉ������̂ŁA���Ƃ��琮������
	//臒l�͎��Ԃ����Ƃɑ������������Ȃ�A�X�̃f�t�H���g�Ɏ��Ԃ�������΂������Ȃ�
	public static final String	TIME_LENGTH							= "TIME_LENGTH";
	public static final long	TIME_LENGTH_DEFAULT					= 5000;//���肷�鎞�ԁA�f�t�H���g�͌ܕb�ԕ��̒l���g��
	public static final String	TIME_DIFFERENCE_THRESHOLD			= "TIME_DIFFERENCE_THRESHOLD";
	public static final long	TIME_DIFFERENCE_THRESHOLD_DEFAULT	= 1000;//msec�A�����x�Ɗp�x�̃Z���T�̎��Ԃ����ꂷ���ĂȂ����Ƃ��F��A�F���Ă��炱���K�v�Ȃ����ƂɋC�Â���
	public static final String	WALK_COUNT							= "WALK_COUNT";//�����v����Pedometer��������A��������Number of step��������B���[�� walk count���Ă��߁H
	public static final String	WALK_COUNT_THRESHOLD 				= "WALK_COUNT_THRESHOLD";
	public static final float	WALK_COUNT_THRESHOLD_DEFAULT 		= 5.4F;//�����x
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
	public static final float	POWER_SAVING_STAIR					= 0.74f;//�K�i���p���̐ߓd�ʁB�P�ʂ́c�H�Ȃ񂾂���?���b�g?
	public static final float	POWER_USING_ELEVATOR				= -3.33f;//�G���x�[�^���p���̏���d��

	public StateInference(Context context) {
		// ��(^��^ )��񁽂������Ԃɍ��������ɂȂ��� 2013/03/10
		// ��(^��^ )��񁽂��񂩂����Ԃɍ��������ɂȂ��� 2013/03/18
		mContext = context;
		sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		mile = sp.getFloat(MILEAGE_POINT, 0);

		// �Z���T�[�ݒ�
		state = "stop";
		inElevator = false;

		//������ǂ�����ǂ��̂�����
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				Log.e("�x��","0");
				inference();// ����
				Log.e("�x��","11");
			}
		}, 0, INTERVAL_PERIOD);
	}

	public void stop() {
		// �������I����͓����Ȃ��҂ł���I
		sp.edit().putFloat(MILEAGE_POINT, mile).commit();
		timer.cancel();
		timer = null;
		// ������ꂽ�\�A���܂���
	}

	public void inference() {//�C���t�@�����X�ł���
		//����ɕK�v�ȕϐ�
		long judgeTime = sp.getLong(TIME_LENGTH, TIME_LENGTH_DEFAULT);//���莞�Ԃ����߂܂�
		//�Z���T�̒l
		long time = java.lang.System.currentTimeMillis();
		ArrayList<SensorData> acceles = LearningDBManager.getSensorData(
				mContext, Sensor.TYPE_ACCELEROMETER, time, judgeTime);
		Log.e("�x��","1.2");
		ArrayList<SensorData> orientations = LearningDBManager.getSensorData(
				mContext, SensorAdmin.TYPE_ORIENTATION_MAKE, time, judgeTime);
		Log.e("�x��","2");
		SimpleDateFormat sdf = new SimpleDateFormat("HH':'mm':'ss'.'SSS");
		Log.e("�f�[�^��","[Accele:"+acceles.size()+"]");
		Log.e("�f�[�^��","[Orient:"+orientations.size()+"]");
		Log.e("���ݎ�",sdf.format(new Date(time)) + "");
		for(int i = 0;i < acceles.size();i++){
			Log.e("Acceles","["+i+"]["+sdf.format(new Date(acceles.get(i).timestamp))+"]" + acceles.get(i).timestamp);
		}
		if(acceles.size() <= sp.getInt(ELEVATOR_DIFFERENT_INTERVAL, ELEVATOR_DIFFERENT_INTERVAL_DEFAULT) || orientations.size() <= 0)	return;//�l���Ȃ���Όv�Z�ł��܂����
		Log.e("�x��","3");
		//���ςƂ����U�Ƃ����������x�Ƃ������Ƃ��A�������̎l�őS���H��ōς�
		ArrayList<Float> verticalAcceles = calcVerticalAcceleration(acceles, orientations);
		Log.e("�x��","4");
		int walkCount = 0;//countWalk(verticalAcceles);
		Log.e("�x��","5");
		this.numSteps += walkCount;
		Log.e("�x��","6");
	}
	
	public void judge(int walkCount, ArrayList<SensorData> acceles, ArrayList<SensorData> orientations, ArrayList<Float> verticalAcceles){//�W���b�W�����g�ł���
		//���肷���[�I
		// ���s�������łȂ����𔻒肵����A���s�̏ꍇ�K�i���ǂ����A���s�łȂ��΂����G���x�[�^�̔�����s��
		if(judgeWalk(walkCount)){
			inElevator = false;// �G���x�[�^���ł͕����Ȃ��悤���肢�\���グ�܂�
			float[] orientationXZVariance = calcOrientationXZVariance(orientations);
			if(judgeStair(orientationXZVariance[0], orientationXZVariance[1])){
				mile += POWER_SAVING_STAIR;//���Ƃő�����ʂ��o�ߎ��Ԃ��݂ĕς��悤�ɒ�������
				state = "stair";
			}
			else	state = "walk";
		}
		else{
			Log.e("�x��","7");
			if(judgeElevator(verticalAcceles, acceles, inElevator)){
				Log.e("�x��","8");
				inElevator = true;// �G���x�[�^���ł���
				mile += POWER_USING_ELEVATOR;
				state = "elevator";// �G���x�[�^�[����
				Log.e("�x��","9");
			} else {
				state = "stop";
			}
		}
		Log.e("�x��","10");
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
	public boolean judgeElevator(ArrayList<Float> verticalAcceles, ArrayList<SensorData> acceles, boolean inElevator){
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

		// �G���x�[�^����
		//�܂��G���x�[�^���㏸���Ă���̂����~���Ă���̂��l����A���̔�������̎c�O���́A�����Ɩ����̒N�������Ƃ����邳
		int featureIndex = 0;//�����̃C���f�b�N�X
		boolean upElevator = true;//�G���x�[�^�����~����Ƃ����������x�͑�������A�t���܂�������Ȃ��񂶂�
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
		boolean upWalk = true;//�����x���㏸���Ă��邩
		float acceleHigh = verticalAcceles.get(0);
		float acceleLow = verticalAcceles.get(0);

		//�����x�̑��������̐��������āA臒l�𒴂����ꍇ����ƃJ�E���g����
		for(float accele : verticalAcceles){
			if (upWalk) {//�����x���㏸����High���X�V�A���~����Low���X�V���ĉ��~����Ɉڂ�
				if (acceleHigh <= accele) acceleHigh = accele;
				else {
					if (acceleHigh - acceleLow > sp.getFloat(WALK_COUNT_THRESHOLD, WALK_COUNT_THRESHOLD_DEFAULT))	walkCount++;
					acceleLow = accele;
					upWalk = false;
				}
			} else {// ���~��������������
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
		//�v�Z�ʌ��炷���߂ɔėp�����]���ɂ����B�����Ƃ������@������͂������A�g���܂킹�Ȃ��֐����Č����Ȃ̂ŁA�C�����������̃I��
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

	public ArrayList<Float> calcVerticalAcceleration(ArrayList<SensorData> accele,ArrayList<SensorData> orientation){// ���������x�̔z��̌v�Z
		ArrayList<Float> verticalAcceles = new ArrayList<Float>();
		int orientationIndex = 0;
		boolean checkIndex = true;
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
}
