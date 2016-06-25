package com.coolweather.app.activity;


import com.coolweather.app.R;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WeatherActivity extends Activity implements View.OnClickListener {
	private LinearLayout weatherInfoLayout;
	private TextView publishText;
	private TextView currentDateText;
	private TextView weatherDespText;
	private TextView temp1Text;
	private TextView temp2Text;
	private TextView cityNameText;
	private Button switchBtn;
	private Button refreshBtn;
	
	private String countyCode;
	
	private static final String TYPE_COUNTY_CODE="countyCode";
	private static final String TYPE_WEATHER_CODE="weatherCode";
	
	public static void WeatherActivityStart(Context context,String countyCode) {
		Intent intent=new Intent(context,WeatherActivity.class);
		intent.putExtra("county_code", countyCode);
		context.startActivity(intent);
	}
	public static void WeatherActivityStart(Context context) {
		Intent intent=new Intent(context,WeatherActivity.class);
		context.startActivity(intent);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		weatherInfoLayout=(LinearLayout)findViewById(R.id.weather_info_layout);
		publishText=(TextView)findViewById(R.id.publish_text);
		currentDateText=(TextView)findViewById(R.id.current_date);
		weatherDespText=(TextView)findViewById(R.id.weather_desp);
		temp1Text=(TextView)findViewById(R.id.temp1);
		temp2Text=(TextView)findViewById(R.id.temp2);
		cityNameText=(TextView)findViewById(R.id.title_text);
		switchBtn=(Button)findViewById(R.id.switch_city);
		refreshBtn=(Button)findViewById(R.id.refresh_weather);
		switchBtn.setEnabled(true);
		refreshBtn.setEnabled(true);
		switchBtn.setVisibility(View.VISIBLE);
		refreshBtn.setVisibility(View.VISIBLE);
		switchBtn.setOnClickListener(this);
		refreshBtn.setOnClickListener(this);
		countyCode=getIntent().getStringExtra("county_code");
		if(!TextUtils.isEmpty(countyCode)){
			publishText.setText(getResources().getString(R.string.synchro_msg));
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryWeatherCode(countyCode);
		}else{
			showWeather();
		}
	}
	
	private void queryWeatherCode(String countyCode){
		String address=getResources().getString(R.string.address_dir_url)
				+countyCode+getResources().getString(R.string.xml_file_suf);
		queryFromServer(address, TYPE_COUNTY_CODE);
	}
	
	private void queryWeatherInfo(String weatherCode){
		String address=getResources().getString(R.string.weather_file_url)
				+weatherCode+getResources().getString(R.string.html_file_suf);
		queryFromServer(address, TYPE_WEATHER_CODE);
	}
	
	private void queryFromServer(final String address,final String type){
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				if(type.equals(TYPE_COUNTY_CODE)){
					if(!TextUtils.isEmpty(response)){
						String[] array=response.split("\\|");
						if(array!=null&&array.length==2){
							String weatherCode=array[1];
							queryWeatherInfo(weatherCode);
						}
					}
				}else if(type.equals(TYPE_WEATHER_CODE)){
					Utility.handleWeatherResponse(WeatherActivity.this, response);
					runOnUiThread(new Runnable() {
						public void run() {
							showWeather();
						}
					});
					
				}
			}
			
			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						String text=getResources().getString(R.string.synchro_failure_text);
						publishText.setText(text);
					}
				});
			}
		});
	}
	
	private void showWeather(){
		SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(this);
		cityNameText.setText(sharedPreferences.getString("city_name", ""));
		weatherDespText.setText(sharedPreferences.getString("weather_desp", ""));
		temp1Text.setText(sharedPreferences.getString("temp1", ""));
		temp2Text.setText(sharedPreferences.getString("temp2", ""));
		String todayText=getResources().getString(R.string.today_text);
		String publishTextLabel=getResources().getString(R.string.publish_text);
		publishText.setText(todayText+sharedPreferences.getString("publish_time", "")+publishTextLabel);
		currentDateText.setText(sharedPreferences.getString("current_date", ""));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.switch_city:
			ChooseAreaActivity.ChooseAreaActivityStart(this, false);
			finish();
			break;
		case R.id.refresh_weather:
			publishText.setText(getResources().getString(R.string.synchro_msg));
			if(!TextUtils.isEmpty(countyCode)){
				queryWeatherCode(countyCode);
			}
			break;

		default:
			break;
		}
		
	}
}
