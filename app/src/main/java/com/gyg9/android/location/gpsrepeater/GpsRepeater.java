package com.gyg9.android.location.gpsrepeater;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.widget.Toast;

import com.gyg9.android.utils.ToastUtil;

/**
 * Created by gyliu on 15/11/5.
 */
public class GpsRepeater {

	private Context mContext;

	private String mTestProvider = "gyliu";
	private String mTargetProvider;
	private LocationManager mLocationManager;
	private MyHandler mHandler;
	private boolean flagRunning = false;

	private Location mRealLocaiton = null;

	private double mLat, mLon;

	public GpsRepeater(Context context, String sourceLP) {
		this.mContext = context;
		this.mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		this.mHandler = new MyHandler(context.getMainLooper());
	}

	public void start(String provider, double lat, double lon) {
		mLat = lat;
		mLon = lon;
		start(provider);
	}

	private void start(String provider) {
		this.mTargetProvider = provider;

		turnOffTestProvider(mTestProvider);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
			turnOnTestProvider(mTestProvider);
			mRealLocaiton = null;
			mHandler.sendMessage(mHandler.obtainMessage(1));
			flagRunning = true;
		}
	}

	public void stop() {
		if (!flagRunning) {
			return;
		}
		mHandler.sendMessage(mHandler.obtainMessage(2));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
			turnOffTestProvider(mTestProvider);
		}
		flagRunning = false;
	}

	public void turnOnTestProvider(String provider) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
			mLocationManager.addTestProvider(provider, true, true, false, false, false, true, true, 1, 1);
			mLocationManager.setTestProviderEnabled(provider, true);
		}
	}

	public void turnOffTestProvider(String provider) {
		try{

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
				if (mLocationManager.isProviderEnabled(provider)) {
					mLocationManager.removeTestProvider(provider);
					//尝试恢复定位数据到真实数据
//					mLocationManager.requestSingleUpdate(provider, null);
				}
			}
		} catch (Exception e) {

		}
	}

	private class MyHandler extends Handler {
		public MyHandler(Looper mainLooper) {
			super(mainLooper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (1 == msg.what) {

				Location mockLocation = new Location(mTargetProvider);
				mockLocation.setTime(System.currentTimeMillis());
				mockLocation.setLongitude(mLon);
				mockLocation.setLatitude(mLat);
				mockLocation.setAccuracy(100);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
					mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
				}

				if (null != mockLocation) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE/*
							&& mLocationManager.isProviderEnabled(mTestProvider)*/
							&& mLocationManager.isProviderEnabled(mTargetProvider)) {
						mLocationManager.setTestProviderLocation(mTestProvider, mockLocation);



						Location l = mLocationManager.getLastKnownLocation(mTargetProvider);
						String info;
						if (null == l) {
							info = "mock location false.";
						} else {
							info = "lat&long:" + l.getLatitude() + l.getLongitude();
						}
						ToastUtil.show(mContext, info);
					}
				} else {
					ToastUtil.show(mContext, "last location = null");
				}

				MyHandler.this.sendMessageDelayed(MyHandler.this.obtainMessage(1), 1000);
			} else if(2 == msg.what) {
				mHandler.removeMessages(1);
			}
		}
	}

}
