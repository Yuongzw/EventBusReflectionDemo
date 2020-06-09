package com.yuong.eventbus.annotation;

import com.yuong.eventbus.annotation.mode.SubscriberInfo;

public interface SubscriberInfoIndex {

    /**
     * 生成索引接口，通过订阅者对象（MainActivity）获取所有订阅方法
     * @param subscriberClass   订阅者对象class，如：MainActivity
     * @return 事件订阅方法的封装类
     */
    SubscriberInfo getSubscriberInfo(Class<?> subscriberClass);
}
