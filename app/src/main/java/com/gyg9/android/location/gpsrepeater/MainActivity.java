package com.gyg9.android.location.gpsrepeater;

import android.content.Context;
import android.content.Loader;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gyg9.android.utils.ToastUtil;

/**
 * just a simple activity
 */
public class MainActivity extends ActionBarActivity {

	GpsRepeater gr;
	Handler mHandler;

	LocationManager lm;

	LocationListener mLocationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			StringBuilder info = new StringBuilder();
			info.append(location.getProvider());
			info.append("\nLat: " + location.getLatitude());
			info.append("\nLong: " + location.getLongitude());
			mHandler.sendMessage(mHandler.obtainMessage(1, info));
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

		@Override
		public void onProviderEnabled(String provider) {

		}

		@Override
		public void onProviderDisabled(String provider) {

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		gr = new GpsRepeater(this, LocationManager.NETWORK_PROVIDER);

		this.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gr.start(LocationManager.GPS_PROVIDER, 30.269862, 120.104618);
			}
		});

		this.findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gr.start(LocationManager.GPS_PROVIDER, 31.999, 121.999);
			}
		});
		this.findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gr.stop();
			}
		});

		this.findViewById(R.id.btn_start_gps).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startLocate(LocationManager.GPS_PROVIDER);
			}
		});

		this.findViewById(R.id.btn_start_network).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startLocate(LocationManager.NETWORK_PROVIDER);
			}
		});

		lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		mHandler = new MyHandler();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void startLocate(String provider) {
		//删除旧的location监听
		lm.removeUpdates(mLocationListener);

		//启动地理位置监听，回调中更新界面
		if (null != lm && lm.isProviderEnabled(provider)) {
			lm.requestLocationUpdates(provider,
					100, 10,
					mLocationListener);
		} else {
			ToastUtil.show(this, "provide " + provider + " DISABLE");
		}

		Location lastLocation = lm.getLastKnownLocation(provider);
		if (null != lastLocation) {
			StringBuilder info = new StringBuilder();
			info.append(lastLocation.getProvider());
			info.append("\nLat: " + lastLocation.getLatitude());
			info.append("\nLong: " + lastLocation.getLongitude());
			mHandler.sendMessage(mHandler.obtainMessage(1, info));
		} else {
			mHandler.sendMessage(mHandler.obtainMessage(1, "null"));
		}

	}

	private class MyHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (1 == msg.what) {
				((TextView) findViewById(R.id.tv_location)).setText(msg.obj.toString());
			}
		}
	}
}
