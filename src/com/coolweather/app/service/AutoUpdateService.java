package com.coolweather.app.service;

import com.coolweather.app.R;
import com.coolweather.app.receiver.AutoUpdateReceiver;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class AutoUpdateService extends Service {

	private static final String TAG=AutoUpdateService.class.getSimpleName();
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent,int flags,int startId){
		new Thread(new Runnable() {
			@Override
			public void run() {
				updateWeather();
			}
		}).start();
		//int delayTime=8*60*60*1000;
		int delayTime=10*1000;//10秒刷新一次
		Intent refreshIntent=new Intent(this,AutoUpdateReceiver.class);
		PendingIntent pi=PendingIntent.getBroadcast(this, 0,refreshIntent, 0);
		setAlarm(delayTime, pi);
		return super.onStartCommand(intent, flags, startId);
	}
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void setAlarm(int alarmDelayTime,PendingIntent pi){
		AlarmManager manager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
		long triggerAtTime=SystemClock.elapsedRealtime()+alarmDelayTime;
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT){
			manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
		}else{
			manager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
		}
		
	}
	
	private void updateWeather(){
		SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(this);
		String weatherCode=sharedPreferences.getString("weather_code", "");
		String address=getResources().getString(R.string.weather_file_url)
				+weatherCode+getResources().getString(R.string.html_file_suf);
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				Utility.handleWeatherResponse(AutoUpdateService.this, response);
				Log.d(TAG, "Service refresh successed");
			}
			
			@Override
			public void onError(Exception e) {
				
			}
		});
	}
	

}
