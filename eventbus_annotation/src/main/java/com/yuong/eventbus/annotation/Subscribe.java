package com.yuong.eventbus.annotation;

import com.yuong.eventbus.annotation.mode.ThreadMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscribe {

    //线程模式，默认是POSTING
    ThreadMode threadMode() default ThreadMode.POSTING;

    //是否使用粘性事件
    boolean sticky() default false;

    //优先级，在同一个线程中，数值越大，优先级越大
    int priority() default 0;
}
