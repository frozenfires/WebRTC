package com.github.vesung.vadsdk;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MediaSyncEvent;
import android.os.Environment;
import android.util.Log;


import com.github.vesung.vadsdk.asr.AsrListener;


import com.github.vesung.vadsdk.asr.AsrLiveEngine;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ArrayBlockingQueue;

public class ApiCloudModule  {
    private static final String TAG = "vadsdk";

    private static ArrayBlockingQueue<byte[]> audioDataQueen = new ArrayBlockingQueue<>(3000);

    public static void putAudioDataQueen(byte[] data){
        try {
            audioDataQueen.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private AsrListener asrListener = new AsrListener() {
        @Override
        public void onResult(String msg) {
            //callbackJs("onmsg", msg);
            Log.d("onresult:","msg:"+msg);
           // MainActivity.setLog(msg);
        }

        @Override
        public void liveBegin() {
            //callbackJs("begin", "");
            Log.d("liveBegin:","liveBegin:");
        }

        @Override
        public void liveEnd() {
           // callbackJs("end", "");
            Log.d("liveEnd:","liveEnd:");
        }
    };



    private AudioRecord mAudioRecord;
    private int m_Record_Size;
    // 线程停止标志
    private boolean stopflag = false;
    // 实时语音识别引擎
    private AsrLiveEngine liveEngine;


 public AsrLiveEngine getLiveEngine(){
     return this.liveEngine;
 }

    public void re_start(){

        Log.d("restart..","re_start........");
        liveEngine = new AsrLiveEngine.Builder()
                .listener(asrListener)
                .api("http://192.168.1.132:8080/asr/baiduresp")
                .build();

        Log.d("restart..","re_start...222222222222222.....");


        // 录音缓存区大小
        m_Record_Size = 160000;

        // 录音对象
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, m_Record_Size);

        Log.d("restart..","re_start..33333333333333333333......");
        // 启动录音线程
//        new RecordThread().start();

       //new LiveAsrThread().start();


        try {
            liveEngine.liveByte(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }







    /**
     * 录音实时处理线程
     */
    private class LiveAsrThread extends Thread{
        private final InputStream voiceInput;

        public LiveAsrThread(InputStream is) {
            this.voiceInput = is;
        }

        public LiveAsrThread(){
            voiceInput = null;
        }

        @Override
        public void run() {
            try {
                Log.i(TAG, "实时音频处理线程已启动");
            // liveEngine.live(new MicInputStream(MediaRecorder.AudioSource.MIC, 16000));

                File file= new File(Environment.getExternalStorageDirectory().getPath()+ "/UZMap/reverseme2.pcm");
                InputStream is = new FileInputStream(file);
                liveEngine.liveFile(is);
                /*
                while(true){

                    byte[]  data = ApiCloudModule.this.audioDataQueen.take();
                     Log.e("take....","take..........");
                    InputStream sbs = new ByteArrayInputStream(data);
                    liveEngine.live(sbs);
                }
                */

               Log.i(TAG, "实时音频处理线程已退出");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 封装录音流
     */
    public static class MicInputStream extends InputStream {
     //   private String TAG = MicrophoneServer.MicInputStream.class.getSimpleName();
        private static final int DEFAULT_BUFFER_SIZE = 160000;
        private AudioRecord mAudioRecord;

        // audioSource, int sampleRateInHz
        public MicInputStream(int audioSource, int sampleRateInHz) {
            try {
                this.mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, 16, 2, 160000);
//                LogUtil.i("audioSource : ", new String[]{var1 + ""});
//                LogUtil.i(this.TAG, new String[]{"startRecordingAndCheckStatus recorder status is " + this.mAudioRecord.getState()});
                this.mAudioRecord.startRecording();
                int var3 = 0;
                byte[] var4 = new byte[32];

                for(int var5 = 0; var5 < 10; ++var5) {
                    int var6 = this.mAudioRecord.read(var4, 0, var4.length);
                    if (var6 > 0) {
                        var3 += var6;
                        break;
                    }
                }

                if (var3 <= 0) {
                    this.mAudioRecord.release();
                    new Exception("bad recorder, read(byte[])");
                }
            } catch (Exception var15) {
                var15.printStackTrace();
            } finally {
                label143: {
                    if (this.mAudioRecord == null || this.mAudioRecord.getRecordingState() == 3) {
                        int var10000 = this.mAudioRecord.getState();
                        AudioRecord var10001 = this.mAudioRecord;
                        if (var10000 != 0) {
                            break label143;
                        }
                    }

                    try {
                        this.mAudioRecord.release();
                    } catch (Exception var14) {
                        var14.printStackTrace();
                    }

//                    LogUtil.d(this.TAG, new String[]{"recorder start failed, RecordingState=" + this.mAudioRecord.getRecordingState()});
                }

            }

        }

        public int read(byte[] var1, int var2, int var3) throws IOException {
            if (this.mAudioRecord == null) {
                throw new IOException("audio recorder is null");
            } else {
                int var4 = this.mAudioRecord.read(var1, var2, var3);
//                LogUtil.v(this.TAG, new String[]{" AudioRecord read: len:" + var4 + " byteOffset:" + var2 + " byteCount:" + var3});
                if (var4 >= 0 && var4 <= var3) {
                    return var4;
                } else {
                    throw new IOException("audio recdoder read error, len = " + var4);
                }
            }
        }

        public void close() throws IOException {
            if (this.mAudioRecord != null) {
                this.mAudioRecord.release();
            }

        }

        public int read() throws IOException {
            throw new IOException("read not support");
        }
    }
}
