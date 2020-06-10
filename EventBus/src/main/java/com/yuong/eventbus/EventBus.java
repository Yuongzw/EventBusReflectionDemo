package com.yuong.eventbus;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.yuong.eventbus.annotation.SubscriberInfoIndex;
import com.yuong.eventbus.annotation.mode.SubscriberInfo;
import com.yuong.eventbus.annotation.mode.SubscriberMethod;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author : zhiwen.yang
 * date   : 2020/6/10
 * desc   :
 * ArrayList的底层是数组，查询和修改直接根据索引可以很快找到对应的元素，而增加和删除就涉及到数组元素的移动，所以会比较慢
 * <p>
 * CopyOnWriteArrayList 实现了List的接口（读写分离）
 * Vector是增删查改方法都加了 synchronized，保证同步，但是每个方法执行的时候都要去获得锁，性能就会大大下降，
 * 而 CopyOnWriteArrayList 只是在增删改上面加锁，但是查不加锁，在查的方面的性能就好与Vector
 */

public class EventBus {
    // volatile 修饰的变量不允许线程内部缓存和重排序，即直接修改内存
    private static volatile EventBus instance;

    //索引接口
    private SubscriberInfoIndex subscriberInfoIndex;

    //订阅者类型集合，如：订阅者MainActivity订阅了哪些EventBean,或者解除了订阅的缓存。
    //key:订阅者，如MainActivity，    value:EventBean集合
    private Map<Object, List<Class<?>>> typesBySubscriber;

    //方法缓存：key：订阅者，如MainActivity，   value：订阅方法集合
    private Map<Object, List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();

    //EventBean缓存：key：Userinfo.class, String.class， value：订阅者（可以说多个Activity）中所有的订阅方法的集合
    private Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionByEventType;

    //粘性事件缓存
    private final Map<Class<?>, Object> stickyEvents;

    private Handler handler;

    private ExecutorService executorService;

    private EventBus() {
        typesBySubscriber = new HashMap<>();
        subscriptionByEventType = new HashMap<>();
        stickyEvents = new HashMap<>();
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
    }

    public static EventBus getDefault() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;
    }


    public void addIndex(SubscriberInfoIndex index) {
        subscriberInfoIndex = index;
    }

    /**
     * 订阅
     * @param subscriber 订阅者
     */
    public void register(Object subscriber) {
        //获取订阅者的class，如：MainActivity.class
        Class<?> subscriberClass = subscriber.getClass();
        //获取订阅方法
        List<SubscriberMethod> subscriberMethods = findAnnotationMethod(subscriberClass);
        synchronized (this) {
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                //订阅
                subscribe(subscriber, subscriberMethod);
            }
        }
    }

    /**
     * 获取含有注解的方法
     * @param subscriberClass 订阅的class，如 MainActivity.class
     * @return
     */
    private List<SubscriberMethod> findAnnotationMethod(Class<?> subscriberClass) {
        //从缓存中读取
        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
        if (null != subscriberMethods) {
            return subscriberMethods;
        }
        //找不到，从apt生成类文件中寻找
        subscriberMethods = findUsingInfo(subscriberClass);
        if (null != subscriberMethods) {
            //存入缓存
            METHOD_CACHE.put(subscriberClass, subscriberMethods);
        }
        return subscriberMethods;
    }

    /**
     * 从 APT 类中寻找方法集合
     * @param subscriberClass 订阅者Class
     * @return
     */
    private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
        if (null == subscriberInfoIndex) {
            throw new RuntimeException("未添加索引方法：addIndex()");
        }
        SubscriberInfo info = subscriberInfoIndex.getSubscriberInfo(subscriberClass);
        if (null != info) {
            return Arrays.asList(info.getSubscriberMethods());
        }
        return null;
    }

    /**
     * 订阅
     * @param subscriber 订阅者
     * @param subscriberMethod 订阅方法
     */
    private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
        //获取方法参数类型，如：UserInfo.class
        Class<?> eventType = subscriberMethod.getEventType();

        //临时存储对象
        Subscription subscription = new Subscription(subscriber, subscriberMethod);

        //读取EventBeans缓存
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionByEventType.get(eventType);
        if (null == subscriptions) {
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionByEventType.put(eventType, subscriptions);
        } else {
            if (subscriptions.contains(subscription)) {
                Log.i("yuongzw", subscriber.getClass() + "重复粘性事件！");
                sticky(subscriberMethod, eventType, subscription);
                return;
            }
        }

        //订阅方法优先级处理，第一次进来肯定是0
        int size = subscriptions.size();
        for (int i = 0; i <= size; i++) {
            //如果满足任一条件则进入循环
            //如果新加入的订阅方法优先级大于集合中所有的订阅方法的优先级，则插队到它之前一位
            if (i == size || subscriberMethod.getPriority() > subscriptions.get(i).subscriberMethod.getPriority()) {
                if (!subscriptions.contains(subscription)) {
                    subscriptions.add(i, subscription);
                }
                break;
            }
        }

        //订阅者类型集合，比如：订阅者MainActivity 订阅了那些EventBean，获取解除订阅的缓存
        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
        if (null == subscribedEvents) {
            subscribedEvents = new ArrayList<>();
            //存入缓存
            typesBySubscriber.put(subscriber, subscribedEvents);
        }
        //注意：subscribe() 方法在遍历过程中，所以一直在添加
        subscribedEvents.add(eventType);

        sticky(subscriberMethod, eventType, subscription);
        //消费后移除掉粘性事件
        removeStickyEvent(eventType);
    }


    /**
     * 判断是否订阅
     * @param subscriber 订阅者
     * @return
     */
    public synchronized boolean isRegistered(Object subscriber) {
        return typesBySubscriber.containsKey(subscriber);
    }

    /**
     * 取消订阅
     * @param subscriber 订阅者
     */
    public void unRegister(Object subscriber) {
        //从缓存中移除
        List<Class<?>> subscriberTypes = typesBySubscriber.get(subscriber);
        if (null != subscriberTypes) {
            subscriberTypes.clear();
            typesBySubscriber.remove(subscriber);
        }
    }


    //抽取原因：可执行多次粘性事件，而不会出现闪退

    /**
     * 粘性事件
     * @param subscriberMethod 方法
     * @param eventType 参数类型
     * @param subscription
     */
    private void sticky(SubscriberMethod subscriberMethod, Class<?> eventType, Subscription subscription) {
        //粘性事件的触发：注册事件就激活方法，因为整个源码只有此处遍历了
        //最佳切入点原因：1、粘性事件的订阅方法加入了缓存；2、注册时只有粘性事件直接激活方法
        //新增开关方法弊端：粘性事件为在缓存中，无法触发订阅方法，且有可能多次执行post() 方法
        if (subscriberMethod.isSticky()) {
            Object stickyEvent = stickyEvents.get(eventType);
            if (null != stickyEvent) {
                postToSubscription(subscription, stickyEvent);
            }
        }
    }


    /**
     * 发送消息 、事件
     * @param event 事件
     */
    public void post(Object event) {
        postSingleEventForEventType(event, event.getClass());
    }

    /**
     * 为EventBean 事件类型发布单个事件
     * @param event 事件
     * @param eventClass 事件类型
     */
    private void postSingleEventForEventType(Object event, Class<?> eventClass) {
        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized (this) {
            subscriptions = subscriptionByEventType.get(eventClass);
        }
        if (null != subscriptions && !subscriptions.isEmpty()) {
            for (Subscription subscription : subscriptions) {
                postToSubscription(subscription, event);
            }
        }
    }

    /**
     * 发送粘性事件
     * @param event 事件
     */
    public void postSticky(Object event) {
        synchronized (stickyEvents) {
            stickyEvents.put(event.getClass(), event);
        }
    }

    /**
     * 发送事件到订阅者的所有订阅方法
     * @param subscription
     * @param event 事件
     */
    private void postToSubscription(final Subscription subscription, final Object event) {
        //匹配订阅方的线程模式
        switch (subscription.subscriberMethod.getThreadMode()) {
            case MAIN:
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    invokeSubscriber(subscription, event);
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            invokeSubscriber(subscription, event);
                        }
                    });
                }
                break;
            case POSTING:
                invokeSubscriber(subscription, event);
                break;
            case BACKGROUND:
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            invokeSubscriber(subscription, event);
                        }
                    });
                } else {
                    invokeSubscriber(subscription, event);
                }
                break;
            case ASYNC:
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        invokeSubscriber(subscription, event);
                    }
                });
                break;
            default:
                break;
        }
    }

    /**
     * 执行订阅方法
     * @param subscription
     * @param event 事件
     */
    private void invokeSubscriber(Subscription subscription, Object event) {
        Method execute =subscription.subscriberMethod.getMethod();
        try {
            execute.invoke(subscription.subscriber, event);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }


    public <T> T getStickyEvent(Class<T> eventType) {
        synchronized (stickyEvents) {
            return eventType.cast(stickyEvents.get(eventType));
        }
    }

    public <T> T removeStickyEvent(Class<T> eventType) {
        synchronized (stickyEvents) {
            return eventType.cast(stickyEvents.remove(eventType));
        }
    }

    public void removeAllStickyEvents() {
        synchronized (stickyEvents) {
            stickyEvents.clear();
        }
    }

}
