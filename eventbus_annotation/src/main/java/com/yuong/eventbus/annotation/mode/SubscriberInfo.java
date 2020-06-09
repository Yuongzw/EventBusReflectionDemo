package com.yuong.eventbus.annotation.mode;

public interface SubscriberInfo {

    //订阅者所属类，比如：MainActivity
    Class<?> getSubscriberClass();

    //获取订阅所属类中所有的订阅事件的方法
    SubscriberMethod[] getSubscriberMethods();
}
