package com.yuong.eventbusreflection;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }

    public void post(View view) {
        Log.i("yuongzw", "Current Thread Name:" + Thread.currentThread().getName());
//        EventBus.getDefault().post(new EventBean(15, "我传递消息了！"));
    }
}
