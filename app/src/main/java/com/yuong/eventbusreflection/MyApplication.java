package com.yuong.eventbusreflection;

import android.app.Application;

import com.yuong.eventbus.EventBus;
import com.yuong.eventbusreflection.apt.EventBusIndex;

/**
 * @author : zhiwen.yang
 * date   : 2020/6/10
 * desc   :
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().addIndex(new EventBusIndex());
    }
}
