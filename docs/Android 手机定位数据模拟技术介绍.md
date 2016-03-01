# Android 手机定位数据模拟技术介绍

[TOC]

## ξ 1 背景

Android 系统通过 [LocationManager](http://developer.android.com/reference/android/location/LocationManager.html) 给应用开发者提供定位能力，一般手机基础能力包括 GPS 定位以及 network 定位。

我们在实际跟定位模块的相关的功能开发过程中。由于定位数据是根据当前手机所处位置得出的。会遇到难于测试、测试发现 bug 难以复现的问题。

> 比如说如果你要测试一个 LBS 相关的“发现周围药店“ 的功能，那么你得首先带手机走到一个周围有药店的位置才能测试。⊙﹏⊙

根据这样的需求，定位数据的模拟成为了我们开发、测试定位相关功能，以及评估其性能的过程中必不可少的技术。

本文主要介绍目前可行的一种定位数据模拟的技术方案

> 作者根据该技术实现了一个可以进行 GPS/ NETWORK 定位数据模拟的测试工具。该工具目前已经共享在github上提供学习。
> 
> [GitHub地址](https://github.com/GYGG/GpsRepeater_Android)

## ξ 2 实现原理

读者如果已经对 Android 的定位功能的基本使用有所了解，可以直接跳至 2.3 节。

一般我们使用系统的定位能力代码如下：

``` java
LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
LocationListener mLocationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			//use location data
            ...
		}
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		@Override
		public void onProviderEnabled(String provider) {}
		@Override
		public void onProviderDisabled(String provider) {}
};

lm.requestLocationUpdates(provider, 100, 10, mLocationListener);
```

其中涉及了"LocationManager","provider"的概念我们下面依次介绍。

### ξ 2.1 LocationManager 基础功能介绍

从 SDK 1 开始，Android 系统已经给我们提供了一个定位模块——LocationManager。

这个类其实是一个 Service 代理，其内部会通过进程间通信来调用系统的 LocationManagerService 来调用定位功能。

我们一般通过`((Context) mContext).getSystemService(Context.LOCATION_SERVICE);`来获取 LocationManager。

LocationManager的几个常用定位API：



### ξ 2.2 LocationProvider 介绍

LocationManager 中，系统是通过一系列 LocationProvider 来实现不同的定位技术，比如 GPS 定位是通过 GpsLocationProvider 来实现的。

每一个Povider 都会对外提供发起定位，以及位置变化回调。

而一般来说，Android 系统只提供一种 provider，那就是 GpsLocationProvider，而并没有提供 Network Location Provider。它一般由第三方应用厂商提供，比如 Google 的 GMS（ Google Mobile Service) 包中有一个 NetworkLocation.Apk 提供了该功能。而国内很多手机则使用了百度的 NetworkLocation_Baidu.apk。它们通过实现系统的 ILocationProviderProxy 接口来让 LocationManagerService 能够管理自己。

我们在使用 LocationManager 时，通过指定 provider 参数来实现指定某种定位技术。

系统原生API的几种 Provider:

* LocationManager.GPS_PROVIDER 
  
* LocationManager.NETWORK_PROVIDER
  
* LocationManager.PASSIVE_PROVIDER
  
  > passive provider 是一个有趣的 provider，其内部不实现任何定位能力，而是通过监听其他 provider 的定位数据的变化来选择他提供的定位数据。
  > 
  > 关于他的技术介绍，可以在“延伸阅读”中找到更多内容。

我们一般通过`((LocationManager) mLocationManager).getProvider(String pProvider);` 来获取各种Provider.

### ξ 2.3 TestLocationProvider

从 Android SDK 3 开始，系统在LocationManager 中集成了 TestProvider 功能。这个功能的几个核心 API 如下：

* LocationManager#addTestProvider(String provider)
* LocationManager#removeTestProvider(String provider)
* LocationManager#setTestProviderLocation(String provider, Location loc)

由这几个 API 可以看出，我们通过调用这几个接口，向 LMS (LocationManagerService) 注册一个测试的 provider 。并且可以通过这个测试的 provder 提供指定的 Location 数据。而这个数据，就是我们可以随意修改的部分。

### ξ 2.4 模拟实战

我们来看看下面这段模拟定位的代码，这段代码示例了模拟一个 LocationManger.NETWORK_PROVIDER 的数据的基本过程。

``` java
// 添加 test provider
String mTestProvider = "fake_provider";
mLocationManager.addTestProvider(mTestProvider, 
		true, true, false, false, false, true, true, 1, 1);
mLocationManager.setTestProviderEnabled(mTestProvider, true);
// 伪造 Locaiton 数据
Location mockLocation = new Location(LocationManager.NETWORK_PROVIDER);
mockLocation.setTime(System.currentTimeMillis());
mockLocation.setLongitude(31);
mockLocation.setLatitude(120);
mockLocation.setAccuracy(100);
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
	mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
}
// 将伪造的 Location 传给 LMS
mLocationManager.setTestProviderLocation(mTestProvider, mockLocation);

//...
//async
//...

// 当不需要伪造 locaiton 后，移出 test provider
mLocationManager.removeTestProvider(mTestProvider);
```

从代码内可以看出，我设置了一个名为 "fake_provider" 的 test provider。 这里 provider 的名字无关紧要，不会影响最终我们模拟的目标 provider。而最关键的是，你需要通过调用 `<init>Location(String provider)` 将你提交的 Location 的 provider 设置为你想要模拟的 provider 。这样最终 LMS 将会把 这个Locaiton 当做目标 provider 的数据。

这里有几个需要特别注意的地方：

1. 不能在提交 Location 之后立即将你的 test provider 移出。
   
   因为
   
2. Location 必须设置 Time 和 Accuracy。
   
   由于在 LMS 中在设置 Location 时会对 Location 对象进行检查，如果发现 Locaiton 数据不完整，将会报异常。

## ξ 3 实战——高德定位 SDK 数据模拟

目前作者负责项目中，使用到了高德定位SDK的「定位」和「地理围栏」功能。所以本次调研的核心目的，是为了将地理位置模拟功能引入高德定位SDK相关功能的模拟测试。

高德 SDK 提供三种定位能力分辨是系统的`LocationManager.GPS_PROVIDER`，`LocationManager.NETWORK_PROVIDER` 和 高德自己的 `LocationProvider.AMapNetwork`  三种。

> 经过地理位置模拟调研的实践，以及高德 SDK 源码逆向分析后。作者发现，高德 SDK 的地理围栏功能，是基于高德自己的 AMapNetwork 进行处理后提供的。目前我们只能模拟 GPS 和 NETWORK 两种 provider 的数据，所以本次暂时不考虑地理围栏模拟的功能。

### ξ 3.1高德SDK 使用介绍

从[高德 SDK 官方文档](http://lbs.amap.com/api/android-location-sdk/guide/location/)知道，高德的定位功能使用方式大概如下代码所示：

先初始化一下定位方式，其中的AMapNetwork，是高德自己的 LocationProvider 。我们后面要模拟GPS。所以模拟实验时候，把这个换成系统的 LocationManager.GPS_PROVIDER。

``` java
/**
* 初始化定位
*/
private void init() {      
  mLocationManagerProxy = LocationManagerProxy.getInstance(this);
  mLocationManagerProxy.requestLocationData(
    LocationProviderProxy.AMapNetwork, 60*1000, 15, this);
}
```

然后通过给高德 SDK 设置一个监听来获取地理位置的回调。

``` java
@Override
public void onLocationChanged(AMapLocation amapLocation) {
  if(amapLocation != null && amapLocation.getAMapException().getErrorCode() == 0){
    //获取位置信息
    Double geoLat = amapLocation.getLatitude();
    Double geoLng = amapLocation.getLongitude();   
  }
}
```

所以简单来说，如果要模拟高德 SDK 的定位，那么只需要在将 GPS 的数据在init 后通过我们的方案来设置为假的位置数据就行了。

### ξ 3.2 模拟方案之架构

在使用高德 SDK 的应用架构上应该分为三层，最上层是我们应用的业务层，中间通过调用高德 SDK 进行地理位置等功能的使用，下层通系统 framework 层的定位相关接口向高德 SDK  提供 GPS 、NETWORK 方式的定位能力。

由于我们的模拟的目的在于进行应用业务逻辑的测试。所以在模拟过程中，不能干预上层业务层和中间层高德 SDK 的本身逻辑。从而必然的，需要一种相对黑盒的模拟方法。所以我们通过使用 GpsRepeater 作为一个第三方应用，在后台对系统的 LocationManager 的定位数据进行模拟。从而达到「部分黑盒」的目的。

具体架构见下图： ![mock location on AMap](/mock.location.on.AMap.svg)

### ξ 3.3 模拟 GPS

通过一段代码来介绍一下定位数据模拟的过程。看看注释就能理解具体思路。

> read the fu*king code please.

``` java
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
	 * @param provider
	 */
	public void turnOnTestProvider(String provider) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
			mLocationManager.addTestProvider(provider, true, true, false, false, false, true, true, 1, 1);
			mLocationManager.setTestProviderEnabled(provider, true);
		}
	}

	/**
	 * 关闭 test provider
	 * @param provider
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


				if (null != mockLocation) {
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
				} else {
					ToastUtil.show(mContext, "last locat``ion = null");
				}
				//需要每隔几秒重新设置一下伪造的数据，保障伪造数据一直在
				MyHandler.this.sendMessageDelayed(MyHandler.this.obtainMessage(1), 1000);
			} else if(2 == msg.what) {
				mHandler.removeMessages(1);
			}
		}
	}

}
```



## 延伸阅读

* 《深入理解 Android- WiFi、NFC 和 GPS 卷》-邓凡平著 机械工业出版社
* [Android Dev 官网资料《 Making Yout App Location-Aware》](http://developer.android.com/training/location/index.html)
* [高德 SDK API ](http://lbs.amap.com/api/android-location-sdk/guide/location/)
