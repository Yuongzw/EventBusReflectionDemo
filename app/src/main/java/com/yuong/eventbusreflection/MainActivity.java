package com.yuong.eventbusreflection;

import android.content.Intent;
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
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
    }

    public void jump(View view) {
        startActivity(new Intent(this, SecondActivity.class));
        EventBus.getDefault().postSticky("我是粘性事件，我在MainActivity中发送。");
    }

    @Subscribe(threadMode = ThreadMode.MAIN, priority = 100)
    public void receiveMsg(EventBean eventBean) {
        Log.i("yuongzw", "Current Thread Name:" + Thread.currentThread().getName());
        Log.e("yuongzw", eventBean.toString());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void receiveMsg2(String str) {
        Log.i("yuongzw", "Current Thread Name:" + Thread.currentThread().getName());
        Log.e("yuongzw", str);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unRegister(this);
        }
    }
}
