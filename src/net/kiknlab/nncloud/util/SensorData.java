package net.kiknlab.nncloud.util;

public class SensorData {
	//SensorEvent���Ďg���Ȃ��̂��ȁ[�A�g�����炱���v��Ȃ��񂾂��ȁ[
	//���Ԃ��ł����炱����e�N���X�ɂ����e��Z���T�̃N���X��邩�ȁ[
	public float[] values;
	public int type;
	public int accuracy;
	public long timestamp;
	
	public SensorData(float[] values, int type, int accuracy, long timestamp){
		//���ƂŁA�l�̃`�F�b�N���Ă��߂�������k������鏈�������A���Ԃ�
		this.values = new float[]{values[0], values[1], values[2]};
		this.type = type;
		this.accuracy = accuracy;
		this.timestamp = timestamp;
	}
}
