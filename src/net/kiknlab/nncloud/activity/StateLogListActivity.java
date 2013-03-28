package net.kiknlab.nncloud.activity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import net.kiknlab.nncloud.R;
import net.kiknlab.nncloud.db.StateLogDBManager;
import net.kiknlab.nncloud.util.StateLog;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class StateLogListActivity extends Activity implements View.OnClickListener{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.state_log_list);
		
		//¡“ú‚ÌŠÔˆÈ‰º‚ğí‚Á‚½unixtime‚ª‚Ù‚µ‚©‚Á‚½‚¾‚¯‚È‚ñ‚¾cèŠÔ‚ª‘½‚¢‹C‚ª‚·‚éc
		Calendar calendar = Calendar.getInstance();
		calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
		calendar.add(Calendar.MILLISECOND, -calendar.get(Calendar.MILLISECOND));
		//ArrayList<StateLog> stateLog = StateLogDBManager.getStateLogList(getApplication());
		ArrayList<StateLog> stateLog = StateLogDBManager.getStateLogListOnDay(getApplication(), calendar.getTimeInMillis());
		Log.e("stateLog", stateLog.size() + "");
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