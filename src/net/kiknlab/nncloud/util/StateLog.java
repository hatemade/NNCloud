package net.kiknlab.nncloud.util;

import net.kiknlab.nncloud.R;

public class StateLog {
	public static final int		STATE_STOP			= 0;
	public static final int		STATE_WALK			= 1;
	public static final int		STATE_STAIR			= 2;
	public static final int		STATE_ELEVATOR		= 3;
	public static final int		NUMBER_OF_STATE		= 4;
	public static final String	STATE_NAME_STOP		= "stop";
	public static final String	STATE_NAME_WALK		= "walk";
	public static final String	STATE_NAME_STAIR	= "stair";
	public static final String	STATE_NAME_ELEVATOR	= "elevator";
	public static final String	STATE_NAME_UNKNOWN	= "unknown";
	public int state;
	public long timestamp;

	public StateLog(int state){
		this.state = state;
		this.timestamp = 0;
	}

	public StateLog(int state, long timestamp){
		this.state = state;
		this.timestamp = timestamp;
	}
	
	public String getStateString(){
		//これって処理速度的にどうなんだろうか、無駄は少ないけど
		return StateLog.getStateString(state);
	}

	public static String getStateString(int state){
		switch(state){
		case STATE_STOP:
			return STATE_NAME_STOP;
		case STATE_WALK:
			return STATE_NAME_WALK;
		case STATE_STAIR:
			return STATE_NAME_STAIR;
		case STATE_ELEVATOR:
			return STATE_NAME_ELEVATOR;
		default:
			return STATE_NAME_UNKNOWN;
		}
	}
	
	public void setStop(long timestamp){
		this.state = StateLog.STATE_STOP;
		this.timestamp = timestamp;
	}
	public void setWalk(long timestamp){
		this.state = StateLog.STATE_WALK;
		this.timestamp = timestamp;
	}
	public void setStair(long timestamp){
		this.state = StateLog.STATE_STAIR;
		this.timestamp = timestamp;
	}
	public void setElevator(long timestamp){
		this.state = StateLog.STATE_ELEVATOR;
		this.timestamp = timestamp;
	}

	public static int getStateIcon(int state) {
		switch(state){
		case STATE_STOP:
			return R.drawable.stop;
		case STATE_WALK:
			return R.drawable.walk;
		case STATE_STAIR:
			return R.drawable.stair;
		case STATE_ELEVATOR:
			return R.drawable.ele;
		default:
			return R.drawable.stop;
		}
	}
}