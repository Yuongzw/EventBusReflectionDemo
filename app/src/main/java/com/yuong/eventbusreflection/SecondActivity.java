package com.yuong.eventbusreflection;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.yuong.eventbus.EventBus;
import com.yuong.eventbus.annotation.Subscribe;
import com.yuong.eventbus.annotation.mode.ThreadMode;
import com.yuong.eventbusreflection.bean.EventBean;

/**
 * @author zhiwen.yang
 */
public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }

    public void post(View view) {
        EventBus.getDefault().register(this);
        Log.i("yuongzw", "Current Thread Name:" + Thread.currentThread().getName());
        EventBus.getDefault().post(new EventBean(15, "我传递消息了！"));
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND, sticky = true)
    public void receivedEvent(String msg) {
        Log.i("yuongzw", "Current Thread Name:" + Thread.currentThread().getName());
        Log.e("yuongzw", "msg:" + msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unRegister(this);
        }
    }
}
