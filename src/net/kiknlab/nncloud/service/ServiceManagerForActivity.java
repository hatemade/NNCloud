package net.kiknlab.nncloud.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

public class ServiceManagerForActivity {
	private Context mContext;
	private FlyToTheCloud mBoundService;
	private boolean mIsBound;

	public ServiceManagerForActivity(Context context){
		mContext = context;
		mIsBound = false;
	}

	public String getTest(){
		if(mIsBound)	return mBoundService.getTest();
		else{
			if(mBoundService == null)	return "Failed";
			else	return "Failed"+mBoundService.getTest();
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundService = ((FlyToTheCloud.FTTCBinder)service).getService();
			mIsBound = true;

			// Tell the user about this for our demo.
			Toast.makeText(mContext.getApplicationContext(), "local_service_connected", Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundService = null;
			Toast.makeText(mContext.getApplicationContext(), "local_service_disconnected", Toast.LENGTH_SHORT).show();
		}
	};

	public void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		mContext.bindService(new Intent(mContext.getApplicationContext(), FlyToTheCloud.class), mConnection, Context.BIND_AUTO_CREATE);
	}

	public void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			mContext.unbindService(mConnection);
			mIsBound = false;
		}
	}

	public void doStartService(){
		mContext.startService(new Intent(mContext.getApplicationContext(), FlyToTheCloud.class));
		this.doBindService();
	}

	public void doStopService(){
		this.doUnbindService();
		mContext.stopService(new Intent(mContext.getApplicationContext(), FlyToTheCloud.class));
	}
}
