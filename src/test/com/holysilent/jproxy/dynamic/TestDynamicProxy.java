package com.holysilent.jproxy.dynamic;

import java.lang.reflect.Proxy;

/**
 *
 * Created by van persie on 2017/1/12.
 */
public class TestDynamicProxy {
    public static void main(String[] args) throws Exception{
        System.getProperties().setProperty("sun.misc.ProxyGenerator.saveGeneratedFiles", "true");
        Object o = Proxy.newProxyInstance(GenericService.class.getClassLoader(),
                new Class[]{GenericService.class, EchoService.class},
                new HSFServiceProxy());
        GenericService g = (GenericService)o;
        Object og = g.$invoke(null, null, null);
    }
}
