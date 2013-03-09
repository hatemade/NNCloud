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
	private SensorManager sensorManager;//�Z���T�[�}�l�[�W��
	private Sensor        accelerometer;//�����x�Z���T�[
	//private Sensor        orientation;//��]�Z���T�[�B���N��̈�������]�Z���T�[�͎��񂾁A�Ȃ����I�H�V�₾���炳
	private Sensor        magnetic;		//���C�Z���T�[
	private Sensor        prox;			//�ߐڃZ���T�[
	private Sensor        light;		//���x�Z���T�[
	//Orientation��deprecated�łȂ���΁A����Ȃɕϐ�����Ȃ�����Ȃ��A�������ʂ����Ȃ�����Ȃ�
	private boolean IsAccele;	//�܂������x���擾�ł��Ă��邩�m�F���܂�
	private boolean IsMagnetic;	//���Ɏ��C���擾�ł��Ă��邩�m�F���܂�
	/* ��]�s�� */
	private static final int MATRIX_SIZE = 16;
	float[]  inR = new float[MATRIX_SIZE];
	float[] outR = new float[MATRIX_SIZE];
	//float[]    I = new float[MATRIX_SIZE];
	/* �Z���T�[�̒l */
	public float[] orientationValues   = new float[3];
	public int orientationAccuracy;
	public float[] magneticValues      = new float[3];
	public float[] accelerometerValues = new float[3];
	//�ݒ�I���I�ɕύX�\�ɂ������i��]���j
	private int sensorSpeed;
	//�v�Z�����肷��悤�ϐ��H�Ȍ��Ȍ��������ꂩ�l����(�L��֥`)
	public double sumTime;
	public double preGetTime, nowGetTime;
	public int times;
	//�ۑ�����H
	private Context mContext;
	private LearningDBManager mDb;//����˂��@�ǋL�F�Ђ悤�����I
	public int getTimes;
	public static final int TransactionPeriod = 2000;

	public SensorAdmin(Object sensorService, Context context) {
		mContext = context;
		
		//�Z���T�X�s�[�h�̒�`�A�I�v�V�����őI�ׂĂ��������ǁA�C���^�[�l�b�g����ŐV�̍œK�Ȓl���擾���Ă�����
		sensorSpeed = SensorManager.SENSOR_DELAY_FASTEST;

		//�Z���T�[�}�l�[�W���̎擾�A��������CHAOS�I���[�I�ɂ�[�I�c�c�c�����Ə����������I�C����������ȁI�ǂ���������������������債�����ׂ���Ȃ��͂�maybe
		//���Ȃ�������
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

		//Orientation������������I
		IsAccele = false;
		IsMagnetic = false;

		//�v�Z�Ȃ�Ƃ�����Ƃ�������
		sumTime = 0;
		nowGetTime = preGetTime = java.lang.System.currentTimeMillis();
		times = 0;
		
		//DB���������ĊJ�n�I
		mDb = new LearningDBManager();
		mDb.beginTransaction(context);
		getTimes = 0;
	}

	public void resume() {
		//�Z���T�[�̏����̊J�n(3)
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

	//�Z���T�[�̏����̒�~
	public void stop() {
		mDb.endTransaction();
		sensorManager.unregisterListener(this);
	}

	//�Z���T�[���X�i�[�̏���
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
			IsMagnetic = false;//�����x���擾��������Ɏ擾�������C��Orientation���v�Z����
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
		
		//�����x�Ǝ��C�g���ČX�����쐬�I
		if(IsAccele&&IsMagnetic){
			SensorManager.getRotationMatrix(inR, null, accelerometerValues, magneticValues);
			SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR);//����ł悩�����̂��H�����񂾂�ȁH�c����ł����c
			SensorManager.getOrientation(outR, orientationValues);//0��yaw�ŁA2��pitch�ŁA1��roll�Ȃ̂Œ��ӁAOrientation�Z���T�[
			orientationAccuracy = (int)(orientationAccuracy/2);
			IsAccele = false;
			IsMagnetic = false;
			mDb.insertTransaction(mContext, new float[]{orientationValues[0],orientationValues[2],orientationValues[1]}, TYPE_ORIENTATION_MAKE, orientationAccuracy, java.lang.System.currentTimeMillis());
			getTimes++;
		}
		
		//���񐔎��s�����̂��g�����U�N�V���������s
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