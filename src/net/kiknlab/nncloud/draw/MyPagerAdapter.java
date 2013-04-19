package net.kiknlab.nncloud.draw;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import net.kiknlab.nncloud.R;
import net.kiknlab.nncloud.db.StateLogDBManager;
import net.kiknlab.nncloud.util.StateLog;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MyPagerAdapter extends PagerAdapter{
	private static int NUM_OF_VIEWS = 100;
	private static Context mContext;
	public final static String DATE_PATTERN		= "yyyy/MM/dd";
	public final static String DATETIME_PATTERN	= "yyyy/MM/dd HH:mm:ss";
	public final static String TIME_PATTERN		= "HH:mm:ss";
	
	public MyPagerAdapter(Context context){
		mContext = context;
	}
	
	@Override
	public int getCount() {
		//Pagerに登録したビューの数を返却。サンプルは固定なのでNUM_OF_VIEWS
		return NUM_OF_VIEWS;
	}

	@Override
	public Object instantiateItem(View collection, int position) {

		LayoutInflater inflater = (LayoutInflater)mContext.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		RelativeLayout stateLogList = (RelativeLayout)inflater.inflate(R.layout.state_log_list_swipe_item, null);

		//DAY TextView
		TextView dayText = (TextView)stateLogList.getChildAt(0);
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.JAPAN);
		dayText.setText(sdf.format(new Date(getDayUnixtime(position))));
		//StateLis ScrollView
		LinearLayout linearList = (LinearLayout)((ScrollView)stateLogList.getChildAt(1)).getChildAt(0);
		setStateLogs(linearList, getDayUnixtime(position));
		//Swipe icon View
		if(position == 0){
			(stateLogList.getChildAt(2)).setBackgroundResource(R.drawable.swipe_icon);
		}

		((ViewPager) collection).addView(stateLogList,0);
		return stateLogList;
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		//ViewPagerに登録していたTextViewを削除する
		((ViewPager) collection).removeView((View)view);
	}
	@Override
	public boolean isViewFromObject(View view, Object object) {
		//表示するViewがコンテナに含まれているか判定する(表示処理のため)
		//objecthainstantiateItemメソッドで返却したオブジェクト。
		//今回はTextViewなので以下の通りオブジェクト比較
		return view == (View)object;
	}

	public long getDayUnixtime(int backToDays){
		//今日の時間以下を削ったunixtimeがほしかっただけなんだ…手間が多い気がする…
		Calendar calendar = Calendar.getInstance();
		Log.e("cindex","" + backToDays);
		calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE) - backToDays, 0, 0, 0);
		calendar.add(Calendar.MILLISECOND, -calendar.get(Calendar.MILLISECOND));
		return calendar.getTimeInMillis();
	}

	public void setStateLogs(LinearLayout layout, long time){
		ArrayList<StateLog> stateLogs = StateLogDBManager.getStateLogListOnDay(mContext.getApplicationContext(), time);

		LayoutInflater inflater = (LayoutInflater)mContext.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		RelativeLayout stateItem;

		SimpleDateFormat sdf = new SimpleDateFormat(TIME_PATTERN, Locale.JAPAN);

		if(stateLogs.size() <= 0){
			TextView text = new TextView(mContext.getApplicationContext());
			text.setText("ログがありません");
			layout.addView(text);
		}
		else{
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
	}
}

