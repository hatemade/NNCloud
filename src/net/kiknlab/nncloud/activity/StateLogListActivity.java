package net.kiknlab.nncloud.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import net.kiknlab.nncloud.R;
import net.kiknlab.nncloud.cloud.CloudManager;
import net.kiknlab.nncloud.db.StateLogDBManager;
import net.kiknlab.nncloud.util.StateLog;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class StateLogListActivity extends Activity implements View.OnClickListener{
	public final static String DATE_PATTERN			= "yyyy/MM/dd HH:mm:ss";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.state_log_list);

		setStateLogs(getDayUnixtime(0));
	}

	public long getDayUnixtime(int backToDays){
		//ç°ì˙ÇÃéûä‘à»â∫ÇçÌÇ¡ÇΩunixtimeÇ™ÇŸÇµÇ©Ç¡ÇΩÇæÇØÇ»ÇÒÇæÅcéËä‘Ç™ëΩÇ¢ãCÇ™Ç∑ÇÈÅc
		Calendar calendar = Calendar.getInstance();
		calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE - backToDays), 0, 0, 0);
		calendar.add(Calendar.MILLISECOND, -calendar.get(Calendar.MILLISECOND));
		return calendar.getTimeInMillis();
	}

	public void setStateLogs(long time){
		ArrayList<StateLog> stateLogs = StateLogDBManager.getStateLogListOnDay(getApplication(), time);

		LinearLayout layout = (LinearLayout)findViewById(R.id.stateLogList);
		LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		RelativeLayout stateItem;

		SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.JAPAN);

		for(int i=0;i < stateLogs.size();i++){
			stateItem = (RelativeLayout)inflater.inflate(R.layout.state_log_item, null);
			ImageView stateImage = (ImageView)stateItem.getChildAt(0);
			TextView stateText = (TextView)stateItem.getChildAt(1);
			TextView stateTime = (TextView)stateItem.getChildAt(2);

			stateImage.setImageResource(StateLog.getStateIcon(stateLogs.get(i).state));
			stateText.setText(StateLog.getStateString(stateLogs.get(i).state));
			stateTime.setText(sdf.format(new Date(stateLogs.get(i).timestamp)));

			layout.addView(stateItem);
		}
	}

	@Override
	public void onStart(){
		super.onStart();
	}

	@Override
	public void onResume(){
		super.onResume();
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
		case 0:
		}
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
	}
}