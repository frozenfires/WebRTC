package com.pine.rtc.util;

import java.util.HashMap;

public class BundleParamsManager {
    public static String ROOM_ID = "roomId";
    public static String SPEECH_URL = "speechUrl";
    public static String ROOM_URL = "roomUrl";
    public static String ROLE = "role";
    public static String COMM_TYPE = "commType";
    public static String SOCKET_URL = "socketUrl";
    public static String REMOTE_ID = "remoteId";
    public static String LOCAL_ID = "localId";
    public static String PACKAGE_NAME = "packageName";

    private static volatile BundleParamsManager bundleParamsManager;
    private HashMap<String,String> bundleParams = new HashMap<>();

    private BundleParamsManager(){

    }

    public static BundleParamsManager getInstance(){
        if (bundleParamsManager == null) {
            synchronized (BundleParamsManager.class) {
                if(bundleParamsManager == null) {
                    bundleParamsManager = new BundleParamsManager();

                }
            }
        }
        return bundleParamsManager;
    }

    public String getParam(String key){
        return bundleParams.get(key);
    }

    public BundleParamsManager addParams(String key,String value){
        if(key!=null){
            bundleParams.put(key,value);
        }
        return this;
    }

    public void clear(){
        bundleParams.clear();
        bundleParamsManager =null;
    }


}
