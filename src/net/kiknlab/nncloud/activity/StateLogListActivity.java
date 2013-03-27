package net.kiknlab.nncloud.activity;

import java.util.Calendar;

import net.kiknlab.nncloud.R;
import net.kiknlab.nncloud.db.StateLogDBManager;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class StateLogListActivity extends Activity implements View.OnClickListener{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.state_log_list);
		
		Calendar calendar = new Calendar();
		StateLogDBManager.getStateLogListOnDay(getApplication(), new Date());
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
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
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