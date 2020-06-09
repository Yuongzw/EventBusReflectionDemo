package com.yuong.eventbusreflection.bean;

public class EventBean {
    private int code;
    private String msg;

    public EventBean(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "EventBean{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}
