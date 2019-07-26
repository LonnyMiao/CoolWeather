package com.coolweather.android;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d("flag", preferences.getString("flag",null));
        Log.d("flag", preferences.getString("weatherNow",null));
        Log.d("flag", preferences.getString("weatherForecast",null));
        Log.d("flag", preferences.getString("weatherLifeStyle",null));
//        preferences.edit().clear().commit();
        if(preferences.getString("weatherNow",null) != null &&
                preferences.getString("weatherForecast",null) != null &&
                preferences.getString("weatherLifeStyle",null) != null &&
                preferences.getString("flag",null).equals("nosearch") ){
            Intent intent = new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
