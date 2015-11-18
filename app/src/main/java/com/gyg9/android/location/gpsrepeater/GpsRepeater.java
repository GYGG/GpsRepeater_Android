package com.gyg9.android.location.gpsrepeater;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import com.gyg9.android.utils.ToastUtil;

/**
 * GPS 模拟器
 * Created by gyliu on 15/11/5.
 */
public class GpsRepeater {

	private Context mContext;

	/**
	 * 这个 string 用于标识我们的test provider 名称.
	 * 名称可以随便起，但是不要跟已知的重复就行，比如不能是"gps"，"network"*/
	private String mTestProvider = "test_provider";
	/**
	 * 目标 provider ，真实存在的。通过开始模拟时由上层指定。
	 * 一般是 {@link LocationManager#GPS_PROVIDER} ,
	 * {@link LocationManager#NETWORK_PROVIDER}
	 */
	private String mTargetProvider;
	private LocationManager mLocationManager;
	private MyHandler mHandler;

	private double mFakeLat, mFakeLon;

	public GpsRepeater(Context context) {
		this.mContext = context;
		this.mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		this.mHandler = new MyHandler(context.getMainLooper());
	}

	/**
	 * 开始模拟位置
	 * @param provider 需要模拟的目标 Provider
	 *                 <p> 可能是
	 *                 {@link LocationManager#GPS_PROVIDER} ,
	 *                 {@link LocationManager#NETWORK_PROVIDER}
	 *                 之一</p>
	 * @param lat 纬度
	 * @param lon 经度
	 */
	public void start(String provider, double lat, double lon) {
		mFakeLat = lat;
		mFakeLon = lon;
		start(provider);
	}

	public void stop() {
		mHandler.sendMessage(mHandler.obtainMessage(2));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
			turnOffTestProvider(mTestProvider);
		}
	}

	/**
	 * 开始模拟定位数据
	 * @param provider 你的 provider 的名称
	 */
	private void start(String provider) {
		this.mTargetProvider = provider;

		//增加 test provider 之前必须先尝试关闭一下，因为可能已经增加过了。
		turnOffTestProvider(mTestProvider);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
			//开启 test provider
			turnOnTestProvider(mTestProvider);
			//在 handler 中真正开始模拟 location 数据
			mHandler.sendMessage(mHandler.obtainMessage(1));
		}
	}

	/**
	 * 打开 test provider
	 * @param provider the name of your test location provider
	 */
	public void turnOnTestProvider(String provider) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
			mLocationManager.addTestProvider(provider, true, true, false, false, false, true, true, 1, 1);
			mLocationManager.setTestProviderEnabled(provider, true);
		}
	}

	/**
	 * 关闭 test provider
	 * @param provider the name of your test location provider
	 */
	public void turnOffTestProvider(String provider) {
		try{

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
				if (mLocationManager.isProviderEnabled(provider)) {
					//不需要调用mLocationManager.setTestProviderEnable(provider, false)
					//直接调用removeTestProvider
					mLocationManager.removeTestProvider(provider);
					//尝试恢复定位数据到真实数据。
					// 在有些手机上，如果不在删除 test provider 后调用request重新获取一下真实数据
					// 会导致调用 {@link LocaitonManager#getLastKnownLocation()} 一直获取到最后一次设置的假数据
//					mLocationManager.requestSingleUpdate(provider, null);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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

				//模拟一个 Locaiton 数据，数据要设全，因为 {@link LocationManagerServicer} 内部会做校验
				Location mockLocation = new Location(mTargetProvider);
				mockLocation.setTime(System.currentTimeMillis());
				mockLocation.setLongitude(mFakeLon);
				mockLocation.setLatitude(mFakeLat);
				mockLocation.setAccuracy(100);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
					mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
				}


				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE/*
						&& mLocationManager.isProviderEnabled(mTestProvider)*/
						&& mLocationManager.isProviderEnabled(mTargetProvider)) {
					mLocationManager.setTestProviderLocation(mTestProvider, mockLocation);


					//看看伪造是否成功
					Location l = mLocationManager.getLastKnownLocation(mTargetProvider);
					String info;
					if (null == l) {
						info = "mock location false.";
					} else {
						info = "lat&long:" + l.getLatitude() + l.getLongitude();
					}
					ToastUtil.show(mContext, info);
				}
				//需要每隔几秒重新设置一下伪造的数据，保障伪造数据一直在
				MyHandler.this.sendMessageDelayed(MyHandler.this.obtainMessage(1), 1000);
			} else if(2 == msg.what) {
				mHandler.removeMessages(1);
			}
		}
	}

}
