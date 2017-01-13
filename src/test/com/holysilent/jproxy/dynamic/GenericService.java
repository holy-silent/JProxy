package com.holysilent.jproxy.dynamic;

/**
 * Created by van persie on 2017/1/13.
 */
public interface GenericService {
    public abstract Object $invoke(String paramString,
                                   String[] paramArrayOfString,
                                   Object[] paramArrayOfObject)
            throws Exception;
}
