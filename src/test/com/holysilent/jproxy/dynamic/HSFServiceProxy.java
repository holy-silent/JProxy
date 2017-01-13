package com.holysilent.jproxy.dynamic;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by van persie on 2017/1/13.
 */
public class HSFServiceProxy implements InvocationHandler {
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("执行RPC远程调用...");
        return new Object();
    }
}
