package com.yuong.eventbusreflection;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.yuong.eventbus.annotation.Subscribe;
import com.yuong.eventbus.annotation.mode.ThreadMode;
import com.yuong.eventbusreflection.bean.EventBean;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        EventBus.getDefault().register(this);
    }

    public void jump(View view) {
        startActivity(new Intent(this, SecondActivity.class));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMsg(EventBean eventBean) {
        Log.i("yuongzw", "Current Thread Name:" + Thread.currentThread().getName());
        Log.e("yuongzw", eventBean.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (EventBus.getDefault().isRegister(this)) {
//            EventBus.getDefault().unRegister(this);
//        }
    }
}
