package com.coolweather.android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.DailyForecastBean;
import com.coolweather.android.gson.LifestyleBean;
import com.coolweather.android.gson.WeatherForecast;
import com.coolweather.android.gson.WeatherLifeStyle;
import com.coolweather.android.gson.WeatherNow;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    final public String key = "&key=ae02dd33e78047df9a4cfb059b3d9d17";
    //实时天气接口
    final static String httpsNow = "https://free-api.heweather.net/s6/weather/now?location=";
    //未来三天天气接口
    final static String httpsForecast = "https://free-api.heweather.net/s6/weather/forecast?location=";
    //生活指数
    final static String httpsLifeStyle = "https://free-api.heweather.net/s6/weather/lifestyle?location=";
    SharedPreferences.Editor editor = null;
    WeatherNow weatherNow = null;
    WeatherForecast weatherForecast = null;
    WeatherLifeStyle weatherLifeStyle = null;

    public SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView humText;
    private TextView flText;
    private TextView sportText;
    private ImageView bingPicImg;
    private Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //修改状态栏
        if(Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }


        setContentView(R.layout.activity_weather);

        //初始化控件
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        humText = findViewById(R.id.aqi_text);
        flText = findViewById(R.id.pm25_text);
        sportText = findViewById(R.id.sport_text);
        bingPicImg = findViewById(R.id.bing_pic_img);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        searchButton = findViewById(R.id.search_city);
        //切换城市按钮
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString("flag","search");
                editor.apply();
                Intent intent = new Intent(WeatherActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherNowString = preferences.getString("weatherNow",null);
        String weatherForecastString = preferences.getString("weatherForecast",null);
        String weatherLifeStyleString = preferences.getString("weatherLifeStyle",null);
        final String weatherId;
        String bingPic = preferences.getString("bing_pic",null);
        if(bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else {
            loadBingPic();
        }
        if(weatherNowString != null && weatherForecastString != null && weatherLifeStyleString != null){
            //有缓存时直接解析天气数据
            WeatherNow weatherNow = Utility.handleWeatherNowResponse(weatherNowString);
            WeatherForecast weatherForecast = Utility.handleWeatherForecastResponse(weatherForecastString);
            WeatherLifeStyle weatherLifeStyle = Utility.handleWeatherLifeStyleResponse(weatherLifeStyleString);
            weatherId = weatherNow.getBasic().getCid();
            showWeatherInfo(weatherNow,weatherForecast,weatherLifeStyle);
        }else{
            //无缓存上服务器查询天气
            weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.VISIBLE);
            requestWeather(weatherId);
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });
    }

    /**
     * 加载图片
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });

    }

    /**
     * 查询天气信息
     * @param weatherId 天气id
     */
    public void requestWeather(final String weatherId) {
        String weatherNowUrl =httpsNow + weatherId + key;
        String weatherForecastUrl = httpsForecast + weatherId + key;
        String weatherLifeStyleUrl = httpsLifeStyle + weatherId + key;
        Log.d("addressNow", ""+weatherNowUrl);
        Log.d("addressForecast", ""+weatherForecastUrl);
        Log.d("addressLifeStyle", ""+weatherLifeStyleUrl);
        //获取实时天气
        HttpUtil.sendOkHttpRequest(weatherNowUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                weatherNow = Utility.handleWeatherNowResponse(responseText);
                Log.d("weahterhaha", ""+weatherNow.getBasic().getAdmin_area());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weatherNow != null && "ok".equals(weatherNow.getStatus())){
                            editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weatherNow",responseText);
                            editor.apply();
                        }else {
                            Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        HttpUtil.sendOkHttpRequest(weatherForecastUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                weatherForecast = Utility.handleWeatherForecastResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weatherForecast != null && "ok".equals(weatherForecast.getStatus())){
                            editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weatherForecast",responseText);
                            editor.apply();
                        }else {
                            Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        HttpUtil.sendOkHttpRequest(weatherLifeStyleUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                weatherLifeStyle = Utility.handleWeatherLifeStyleResponse(responseText);
                //Log.d("weahterheihei", ""+weatherLifeStyle.getBasic().getAdmin_area());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weatherNow != null && weatherForecast != null && weatherLifeStyle != null && "ok".equals(weatherLifeStyle.getStatus())){
                            editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weatherLifeStyle",responseText);
                            editor.putString("flag","nosearch");
                            editor.apply();
                            showWeatherInfo(weatherNow, weatherForecast, weatherLifeStyle);
                        }else {
                            Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }
    //处理并展示weather实体类中数据
    private void showWeatherInfo(WeatherNow weatherNow,WeatherForecast weatherForecast,WeatherLifeStyle weatherLifeStyle) {
        String cityName = weatherNow.getBasic().getLocation();
        String updateTime = weatherNow.getUpdate().getLoc().split(" ")[1];
        String degree = weatherNow.getNow().getTmp()+"℃";
        String weatherInfo = weatherNow.getNow().getCond_txt();
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (DailyForecastBean forecast : weatherForecast.getDaily_forecast()) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dataText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dataText.setText(forecast.getDate());
            infoText.setText(forecast.getCond_txt_d()+"-"+forecast.getCond_txt_n());
            maxText.setText(forecast.getTmp_max());
            minText.setText(forecast.getTmp_min());
            forecastLayout.addView(view);
        }

        humText.setText(weatherNow.getNow().getHum());
        flText.setText(weatherNow.getNow().getFl());
        StringBuilder lifeSytleText = new StringBuilder();
        for (LifestyleBean lifeStyle:weatherLifeStyle.getLifestyle()) {
            lifeSytleText.append("类型：").append(lifeStyle.getType()).append("\n");
            lifeSytleText.append("程度：").append(lifeStyle.getBrf()).append("\n");
            lifeSytleText.append("建言：").append(lifeStyle.getTxt()).append("\n");
        }
        sportText.setText(lifeSytleText);
        weatherLayout.setVisibility(View.VISIBLE);
    }
}
