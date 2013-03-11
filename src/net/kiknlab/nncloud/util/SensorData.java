package net.kiknlab.nncloud.util;

public class SensorData {
	//SensorEventって使えないのかなー、使えたらこいつ要らないんだがなー
	//時間ができたらこいつを親クラスにした各種センサのクラス作るかなー
	public float[] values;
	public int type;
	public int accuracy;
	public long timestamp;
	
	public SensorData(float[] values, int type, int accuracy, long timestamp){
		//あとで、値のチェックしてだめだったらヌル入れる処理かく、たぶん
		this.values = values;
		this.type = type;
		this.accuracy = accuracy;
		this.timestamp = timestamp;
	}
}
