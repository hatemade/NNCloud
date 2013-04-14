package net.kiknlab.nncloud.activity;

import net.kiknlab.nncloud.R;
import net.kiknlab.nncloud.db.LearningDBManager;
import net.kiknlab.nncloud.db.LogToData;
import net.kiknlab.nncloud.draw.NakedView;
import net.kiknlab.nncloud.service.ServiceManagerForActivity;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class NNCloudActivity extends Activity implements View.OnClickListener, Runnable{
	private ServiceManagerForActivity mServiceManager;
	private NakedView nakedView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nncloud);

		//Serviceをプロデュースするマネージャを雇います
		mServiceManager = new ServiceManagerForActivity(this);

		//ボタン設定
		//TextView startTxt = (TextView) findViewById(R.id.StartServiceText);
		//ImageView startIcn = (ImageView) findViewById(R.id.StartServiceIcon);
		View startBtn = findViewById(R.id.StartServiceButton);
		Button stopBtn = (Button) findViewById(R.id.stop);
		ImageView elevBtn = (ImageView)findViewById(R.id.elev_button);
		ImageView walkBtn = (ImageView)findViewById(R.id.walk_button);
		ImageView stairBtn = (ImageView)findViewById(R.id.stir_button);
		startBtn.setId(0);startBtn.setOnClickListener(this);
		stopBtn.setId(1);stopBtn.setOnClickListener(this);
		elevBtn.setId(2);elevBtn.setOnClickListener(this);
		//walkBtn.setId(3);walkBtn.setOnClickListener(this);
		stairBtn.setId(4);stairBtn.setOnClickListener(this);

		//SurfaceView
		FrameLayout nakedLog = (FrameLayout)findViewById(R.id.LogSurface);
		nakedView = new NakedView(this, new Thread(this));
		nakedLog.addView(nakedView);

		//LearningDBManager.getAllSensorData(getApplicationContext());
	}

	@Override
	public void onStart(){
		super.onStart();
	}

	@Override
	public void onResume(){
		super.onResume();
		nakedView.mThread = null;
		nakedView.mThread = new Thread(this);
	}

	@Override
	public void run() {
		while(nakedView.mThread!=null) {
			nakedView.draw(
					mServiceManager.getTest());
			//Log.e("serviceTest", mServiceManager.getTest());
			try {
				Thread.sleep(250);
			} catch (Exception e) {
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
		case 0:
			Log.e("fight", "1 kita!");
			mServiceManager.doStartService();
			Log.e("fight", "1 owata! dekita:"+mServiceManager.getTest());
			view.setId(1);
			break;
		case 1:
			Log.e("fight", "2 kita!");
			mServiceManager.doStopService();
			Log.e("fight", "2 owata!");
			LinearLayout a = (LinearLayout)view;
			view.setId(0);
			break;
		case 2:
			//SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			//new JoinServerTask(this).execute();
			//Log.e("NAME",sp.getString("PREF_USER", "test"));
			//LoginServerTask.checkSession(this);
			//new LoginServerTask(this).execute();
			//Log.e("SESSION",sp.getString("PREF_SESSION", "test"));
			//new SendSensorServerTask(this).execute();
			//LearningDBManager.getSensorData(getApplicationContext(),1);
			LearningDBManager.countDats(this);
			break;
		case 3:
			Log.e("File kakikomi","1");
			new LogToData(this).execute();
			break;
		case 4:
			Log.e("activity kidou","hummmmm");
			Intent intent = new Intent(NNCloudActivity.this, StateLogListActivity.class);
			startActivity(intent);
			break;
		}
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
	}
}