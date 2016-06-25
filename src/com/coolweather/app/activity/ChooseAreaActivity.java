package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import com.coolweather.app.R;
import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity{
	
	public static final int LEVEL_PROVINCE=0;
	public static final int LEVEL_CITY=1;
	public static final int LEVEL_COUNTY=2;
	private int currentLevel;
	private boolean checkCity;
	
	public static final String TYPE_PROVINCE="province";
	public static final String TYPE_CITY="city";
	public static final String TYPE_COUNTY="county";
	
	private ListView listView;
	private TextView titleText;
	
	private CoolWeatherDB coolWeatherDB;
	private ProgressDialog progressDialog;
	
	private ArrayAdapter<String> adapter;
	private List<String> dataList=new ArrayList<String>();
	private List<Province> provinceList;
	private List<City> cityList;
	private List<County> countyList;
	private Province selectedProvince;
	private City selectedCity;
	
	public static void ChooseAreaActivityStart(Context context,boolean checkCitySelected){
		Intent intent=new Intent(context,ChooseAreaActivity.class);
		intent.putExtra("checkCitySelected", checkCitySelected);
		context.startActivity(intent);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		checkCity=getIntent().getBooleanExtra("checkCitySelected", true);
		if(checkCity&&checkCitySelected()){
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView=(ListView)findViewById(R.id.list_view);
		titleText=(TextView)findViewById(R.id.title_text);
		adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
				dataList);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(currentLevel==LEVEL_PROVINCE){
					selectedProvince=provinceList.get(position);
					queryCities();
				}else if(currentLevel==LEVEL_CITY){
					selectedCity=cityList.get(position);
					queryCounties();
				}else if(currentLevel==LEVEL_COUNTY){
					WeatherActivity.WeatherActivityStart(ChooseAreaActivity.this,
							countyList.get(position).getCountyCode());
					finish();
				}
			}
		});
		coolWeatherDB=CoolWeatherDB.getInstance(this);
		queryProvinces();
	}
	
	private boolean checkCitySelected(){
		SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(this);
		boolean citySelected=sharedPreferences.getBoolean("city_selected", false);
		if(citySelected){
			WeatherActivity.WeatherActivityStart(this);
			finish();
			return true;
		}
		return false;
	}
	
	private void queryProvinces(){
		provinceList=coolWeatherDB.loadProvinces();
		if(provinceList.size()>0){
			dataList.clear();
			for(Province p:provinceList){
				dataList.add(p.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(getResources().getString(R.string.country_name));
			currentLevel=LEVEL_PROVINCE;
		}else{
			queryFromServer(null,TYPE_PROVINCE);
		}
	}
	
	private void queryCities(){
		cityList=coolWeatherDB.loadCities(selectedProvince.getId());
		if(cityList.size()>0){
			dataList.clear();
			for(City c:cityList){
				dataList.add(c.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel=LEVEL_CITY;
		}else{
			queryFromServer(selectedProvince.getProvinceCode(),TYPE_CITY);
		}
	}
	
	private void queryCounties(){
		countyList=coolWeatherDB.loadCounties(selectedCity.getId());
		if(countyList.size()>0){
			dataList.clear();
			for(County c:countyList){
				dataList.add(c.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel=LEVEL_COUNTY;
		}else{
			queryFromServer(selectedCity.getCityCode(),TYPE_COUNTY);
		}
	}
	
	private void queryFromServer(final String code,final String type){
		String address;
		if(!TextUtils.isEmpty(code)){
			address=getResources().getString(R.string.address_dir_url)+code+
					getResources().getString(R.string.xml_file_suf);
		}else{
			address=getResources().getString(R.string.address_file_url);
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				boolean result=false;
				if(type.equals(TYPE_PROVINCE)){
					result=Utility.handleProvincesResponse(coolWeatherDB, response);
				}else if(type.equals(TYPE_CITY)){
					result=Utility.handleCitysResponse(coolWeatherDB, response, selectedProvince.getId());
				}else if(type.equals(TYPE_COUNTY)){
					result=Utility.handleCountiesResponse(coolWeatherDB, response, selectedCity.getId());
				}
				if(result){
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							closeProgressDialog();
							if(type.equals(TYPE_PROVINCE)){
								queryProvinces();
							}else if(type.equals(TYPE_CITY)){
								queryCities();
							}else if(type.equals(TYPE_COUNTY)){
								queryCounties();
							}
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, 
								getResources().getString(R.string.load_data_failure), Toast.LENGTH_SHORT).show();
					}
				});
				
			}
		});
	}
	
	private void showProgressDialog(){
		if(progressDialog==null){
			progressDialog=new ProgressDialog(this);
			progressDialog.setMessage(getResources().getString(R.string.loading_data_msg));
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	private void closeProgressDialog(){
		if(progressDialog!=null){
			progressDialog.dismiss();
		}
	}
	
	@Override
	public void onBackPressed(){
		switch (currentLevel) {
		case LEVEL_CITY:
			queryProvinces();
			break;
		case LEVEL_COUNTY:
			queryCities();
			break;
		default:
			if(checkCity){
				WeatherActivity.WeatherActivityStart(this);
			}
			finish();
			break;
		}
	}
}
