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
	public final static int TYPE_ORIENTATION_MAKE = 3;//orientation��deprecated�Ȃ̂Ŏ����Œ�`
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
	public float[] accelerometerValues   = new float[3];
	public float[] magneticValues   = new float[3];
	public float[] orientationValues   = new float[3];
	public int orientationAccuracy;
	public ArrayList<SensorData> accelerometerDatas;
	public ArrayList<SensorData> magneticDatas;
	public ArrayList<SensorData> orientationDatas;
	//�ݒ�I���I�ɕύX�\�ɂ������i��]���j
	private int sensorSpeed;
	//�ۑ�����H
	//private Context mContext;
	//private LearningDBManager mDb;//����˂��@�ǋL�F�Ђ悤�����I �ǋL:����Ȃ�����
	//public static final int TransactionPeriod = 800;

	public SensorAdmin(Object sensorService, Context context) {
		//mContext = context;

		//�Z���T�X�s�[�h�̒�`�A�I�v�V�����őI�ׂĂ��������ǁA�C���^�[�l�b�g����ŐV�̍œK�Ȓl���擾���Ă������A���Ă݂���
		//�悭�悭���Ă݂���A�f�B���C�̑���int�Őݒ�ł���̂�microsec���[�A��H�܂�����H
		//�Ȃ񂩁A���̃A�v���ŃZ���T�[�擾���Ă�ƁA�������Őݒ肳�ꂽ���������f�����H�p����
		sensorSpeed = 100000;//SensorManager.SENSOR_DELAY_FASTEST;

		//�Z���T�[�}�l�[�W���̎擾�A��������CHAOS�I���[�I�ɂ�[�I�c�c�c�����Ə����������I�C����������ȁI�ǂ���������������������債�����ׂ���Ȃ��͂�maybe
		//���Ȃ�������
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

		//Orientation������������I
		IsAccele = false;
		IsMagnetic = false;

		//Sensor�̒l���i�[����ArrayList
		accelerometerDatas = new ArrayList<SensorData>();
		magneticDatas = new ArrayList<SensorData>();
		orientationDatas = new ArrayList<SensorData>();
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
		sensorManager.unregisterListener(this);
	}

	//�Z���T�[���X�i�[�̏���
	@Override
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType()){
		case Sensor.TYPE_ACCELEROMETER:
			accelerometerDatas.add(new SensorData(event.values, event.sensor.getType(), event.accuracy, java.lang.System.currentTimeMillis()));
			//Log.e("values2", "" + accelerometerDatas.get(0).values[1]);//accelerometerDatas.size() - 1).values[1]);
			accelerometerValues = event.values;
			orientationAccuracy = event.accuracy;
			IsAccele = true;
			IsMagnetic = false;//�����x���擾��������Ɏ擾�������C��Orientation���v�Z����
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

		//�����x�Ǝ��C�g���ČX�����쐬�I
		if(IsAccele&&IsMagnetic){
			SensorManager.getRotationMatrix(inR, null, accelerometerValues, magneticValues);
			SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR);//����ł悩�����̂��H�����񂾂�ȁH�c����ł����c
			SensorManager.getOrientation(outR, orientationValues);//0��yaw�ŁA2��pitch�ŁA1��roll�Ȃ̂Œ��ӁAOrientation�Z���T�[
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