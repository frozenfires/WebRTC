package com.github.vesung.vadsdk;

import android.util.Log;

import com.github.vesung.vadsdk.asr.AsrListener;
import com.github.vesung.vadsdk.asr.AsrLiveEngine;


/**
 * 语音识别引擎管理器
 */
public class SpeechRecognitionManager {


    private static SpeechRecognitionManager instanceManager =new SpeechRecognitionManager();

    //识别引擎
    private  AsrLiveEngine liveEngine =null;

    //识别服务器地址
    private String speechRecognitionURL=null;

    //识别监听器
    private AsrListener asrListener=null;

    public static SpeechRecognitionManager    getInstance(){
            return instanceManager;
    }


    public void setSpeechRecognitionURL(String url){
         this.speechRecognitionURL=url;
    }


    public AsrListener getAsrListener() {
        return asrListener;
    }

    public void setAsrListener(AsrListener asrListener) {
        this.asrListener = asrListener;
    }

    public void init(){
        Log.d("SpeechManager.","init..");
        liveEngine = new AsrLiveEngine.Builder()
                .listener(asrListener)
                .api(this.speechRecognitionURL)
                .build();

    }


    public void startliveByte(){
        try {
            this.liveEngine.liveByte(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void putByteData(byte[] byteData){

        liveEngine.putAudioDataQueen(byteData);

    }


}
