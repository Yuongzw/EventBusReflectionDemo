package com.yuong.eventbus;

import androidx.annotation.Nullable;

import com.yuong.eventbus.annotation.mode.SubscriberMethod;

/**
 * @author : zhiwen.yang
 * date   : 2020/6/10
 * desc   :
 */
public class Subscription {
    final Object subscriber;    //订阅者，如：MainActivity.class

    final SubscriberMethod subscriberMethod;    //订阅方法

    public Subscription(Object subscriber, SubscriberMethod subscriberMethod) {
        this.subscriber = subscriber;
        this.subscriberMethod = subscriberMethod;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        //必须重写方法，检测激活粘性事件重复调用（同一个对象注册多个）
        if (obj instanceof Subscription) {
            Subscription otherSubscription = (Subscription) obj;
            //删除官方的EventBus：subscriber == otherSubscription.subscriber
            //原因：粘性事件bug，多次调用和移除时出现，参考 Subscription.java 37行
            return subscriberMethod.equals(otherSubscription.subscriberMethod);
        } else {
            return false;
        }
    }
}
