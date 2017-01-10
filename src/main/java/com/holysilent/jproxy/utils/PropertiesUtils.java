package com.holysilent.jproxy.utils;

import com.holysilent.jproxy.constant.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by silent on 2017/1/8.
 */
public class PropertiesUtils {
    private static Properties p = new Properties();

    static {
        try {
            InputStream in = PropertiesUtils.class.getClassLoader().getResourceAsStream(Constants.INIT_FILE);
            p.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return p.getProperty(key);
    }

    public static void set(String key, String value) {
        p.setProperty(key, value);
    }
}
