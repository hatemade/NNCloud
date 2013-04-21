package net.kiknlab.nncloud.activity;

import net.kiknlab.nncloud.R;
import net.kiknlab.nncloud.db.LearningDBManager;
import net.kiknlab.nncloud.db.LogToData;
import net.kiknlab.nncloud.draw.MyPagerAdapter;
import net.kiknlab.nncloud.draw.NakedView;
import net.kiknlab.nncloud.service.ServiceManagerForActivity;
import net.kiknlab.nncloud.util.StateLog;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class NNCloudActivity extends Activity implements View.OnClickListener, Runnable{
	private ServiceManagerForActivity mServiceManager;
	private TextView appStateRunningText;
	private NakedView nakedView;
	public Handler handler;
	private TextView appStateStep;
	private TextView appStateMile;
	private ImageView appStateIcon;
	private boolean issetClickListenerMenubtn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nncloud);

		//ボタン設定
		View menuBtn = findViewById(R.id.AppBarMenuButton);
		menuBtn.setId(3);menuBtn.setOnClickListener(this);
		issetClickListenerMenubtn = false;

		mServiceManager = new ServiceManagerForActivity(this);
		appStateRunningText = (TextView)findViewById(R.id.AppStateText);
		//Serviceをプロデュースするマネージャを雇います
		if(mServiceManager.isServiceRunning()){
			mServiceManager.doBindService();
			setStopServiceButton((LinearLayout)findViewById(R.id.StartServiceButton));
		}
		else	setStartServiceButton((LinearLayout)findViewById(R.id.StartServiceButton));

		//SurfaceViewデバッグ用だからあとで消そうねー
		FrameLayout nakedLog = (FrameLayout)findViewById(R.id.LogSurface);
		nakedView = new NakedView(this, new Thread(this));
		nakedLog.addView(nakedView);

		handler = new Handler();
		appStateStep = (TextView)findViewById(R.id.AppStateStep);
		appStateMile = (TextView)findViewById(R.id.AppStateMile);
		appStateIcon = (ImageView)findViewById(R.id.AppStateIcon);
	}

	@Override
	public void onStart(){
		super.onStart();
		MyPagerAdapter mPagerAdapter;
		ViewPager mViewPager;
		mPagerAdapter = new MyPagerAdapter(this);
		mViewPager = (ViewPager) findViewById(R.id.stateListPager);
		mViewPager.setAdapter(mPagerAdapter);
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
			handler.post(new Runnable() {
				@Override
				public void run() {
					String[] inferenceInfo = mServiceManager.getTest().split(":");
					if(inferenceInfo.length == 3){
						int parse = Integer.parseInt(inferenceInfo[0]);
						int id = StateLog.getStateIcon(parse);
						appStateIcon.setImageResource(id);
						appStateStep.setText("歩数：" + inferenceInfo[1]);
						appStateMile.setText("マイル：" + inferenceInfo[2]);
					}
				}
			});

			//nakedView.draw();
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

	private void setStartServiceButton(LinearLayout startBtn){
		startBtn.setId(0);
		if(!issetClickListenerMenubtn){startBtn.setOnClickListener(this);issetClickListenerMenubtn = true;}
		((ImageView)startBtn.getChildAt(0)).setImageResource(R.drawable.ic_start);
		((TextView)startBtn.getChildAt(1)).setText(R.string.start_service);
		appStateRunningText.setText("動作停止中");
	}

	private void setStopServiceButton(LinearLayout startBtn){
		startBtn.setId(1);
		if(!issetClickListenerMenubtn){startBtn.setOnClickListener(this);issetClickListenerMenubtn = true;}
		((ImageView)startBtn.getChildAt(0)).setImageResource(R.drawable.ic_stop);
		((TextView)startBtn.getChildAt(1)).setText(R.string.stop_service);
		appStateRunningText.setText("動作中");
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
		case 0:
			mServiceManager.doStartService();
			setStopServiceButton((LinearLayout)view);
			break;
		case 1:
			mServiceManager.doStopService();
			setStartServiceButton((LinearLayout)view);
			break;
		case 2:
			LearningDBManager.countDats(this);
			break;
		case 3:
			new LogToData(this).execute();
			break;
		case 4:
			Intent intent = new Intent(NNCloudActivity.this, StateLogListActivity.class);
			startActivity(intent);
			break;
		}
	}

	@Override
	protected void onDestroy(){
		mServiceManager.doUnbindService();
		super.onDestroy();
	}
}