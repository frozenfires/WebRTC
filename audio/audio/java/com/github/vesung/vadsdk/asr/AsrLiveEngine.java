package com.github.vesung.vadsdk.asr;

import android.util.Base64;
import android.util.Log;

import com.example.a123.myapp.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 异步实时语音识别引擎
 * Created by wangjing.dc on 2018/9/12.
 */

public class AsrLiveEngine {



    static {
        System.loadLibrary("vadsdk");
    }

    // MULTI = 1 2 3 webrtc vad可以设置分别为以10ms 20ms 30ms作为包
    private static final int MULTI = 1;
    // 定义frame大小
    private static final int framesize = 160 * MULTI;

    // 语音识别监听
    private AsrListener asrListener;
    // 语音识别服务地址
    private String apiurl;
    // 语音异步识别队列
    private ArrayBlockingQueue<PCMFile> asrAsyncQueen = new ArrayBlockingQueue<>(3000);
    // 语音异步识别线程
    private AsrLiveThread asrLiveThread = new AsrLiveThread();
    private OkHttpClient httpClient;

    //识别标识
    private boolean closeFlag=false;

    private void liveOpen() throws Exception {
        this.vadOpen();
    }


    /**
     * 设置
     * @param
     */
    public void closeLiveByte(){
        this.closeFlag=true;
    }


    private  ArrayBlockingQueue<byte[]> audioDataQueen = new ArrayBlockingQueue<>(3000);

    public  void putAudioDataQueen(byte[] data){
        try {
            audioDataQueen.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 实时识别语音流 针对实时识别的业务场景
     * @param input pcm格式的语音流
     * @throws Exception
     */
    public void liveRecord(InputStream input) throws Exception {
        if(!asrLiveThread.isAlive())
            asrLiveThread.start();

        liveOpen();

        VadBuffer vadBuffer = new VadBuffer();
        byte[] buffer = new byte[framesize * 2];
        int read = input.read(buffer);
        Log.e("live read..","read:"+read);
        if(read != -1)

              this.asrListener.liveBegin();
        while(read != -1){

            vadBuffer.add(buffer);
            String ret = this.vadProcessFrame(buffer);
            if(! ret.equals("0")){
                PCMFile pcmFile = new PCMFile();
                pcmFile.setFileName(ret);
                pcmFile.setFileContent(vadBuffer.asByte());
                asrAsyncQueen.put(pcmFile);
                vadBuffer = new VadBuffer();
            }

            read = input.read(buffer);
        }

        // 结束后放入null，结束线程
        asrAsyncQueen.put(new AsrThreadCloseFlag());
        this.liveClose();
    }


    /**
     * 实时识别语音流,对文件进行识别
     * @param input pcm格式的语音流
     * @throws Exception
     */
    public void liveFile(InputStream input) throws Exception {
        if(!asrLiveThread.isAlive())
            asrLiveThread.start();

        liveOpen();

        VadBuffer vadBuffer = new VadBuffer();
        byte[] mBuffer = new byte[framesize * 2];

        this.asrListener.liveBegin();

        int read =input.read(mBuffer);
        while (MainActivity.getIsRecording()&&read != -1){

            vadBuffer.add(mBuffer);
            String ret = this.vadProcessFrame(mBuffer);
            if(! ret.equals("0")){
                PCMFile pcmFile = new PCMFile();
                pcmFile.setFileName(ret);
                pcmFile.setFileContent(vadBuffer.asByte());
                asrAsyncQueen.put(pcmFile);
                vadBuffer = new VadBuffer();
            }

            read=input.read(mBuffer);
        }


        // 结束后放入null，结束线程
        asrAsyncQueen.put(new AsrThreadCloseFlag());
        this.liveClose();


    }





    /**
     * 实时识别语音流 参数为字节数组
     * @param input pcm格式的语音流
     * @throws Exception
     */
    public void liveByte(InputStream input) throws Exception {
        if(!asrLiveThread.isAlive())
            asrLiveThread.start();

        liveOpen();


        this.asrListener.liveBegin();

        new Thread() {
            @Override
            public void run() {
                Log.e("liveByte....","run..........");
                VadBuffer vadBuffer = new VadBuffer();
                byte[] mBuffer = new byte[framesize * 2];

                try {
                while (true){

                        if(closeFlag){

                            // 结束后放入null，结束线程
                               asrAsyncQueen.put(new AsrThreadCloseFlag());
                               liveClose();

                        }

                        if(audioDataQueen.size()>0) {

                            byte[] data = audioDataQueen.take();

                            vadBuffer.add(data);
                            String ret = vadProcessFrame(data);

                            if (!ret.equals("0")) {
                                PCMFile pcmFile = new PCMFile();
                                pcmFile.setFileName(ret);
                                pcmFile.setFileContent(vadBuffer.asByte());
                                asrAsyncQueen.put(pcmFile);
                                vadBuffer = new VadBuffer();
                            }

                        }

                }



                }catch(Exception e){ Log.e("liveByte error:","error"+e.getMessage());}

            }

        }.start();



    }


    /**
     * 关闭
     */
    private void liveClose(){
        try {
            this.vadClose();
        }catch (Exception e) {
            Log.e(this.getClass().getName(), "识别引擎关闭异常", e);
        }

    }

    /**
     * 发送post请求
     * @param fileContent
     * @return
     */
    private String post(String url, byte[] fileContent) {
        FormBody fbody = new FormBody.Builder()
                .add("pcm", Base64.encodeToString(fileContent, Base64.DEFAULT))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(fbody)
                .build();
        try {
            Response res = httpClient.newCall(request).execute();
            return res.body().string();
        } catch (IOException e) {
            Log.e("MainActivity", "通讯失败", e);
        }

        return null;
    }

    /**
     * 开启Vad识别
     * @throws Exception
     */
    public native void vadOpen() throws Exception;

    /**
     * 关闭vad识别
     * @throws Exception
     */
    public native void vadClose() throws Exception;

    /**
     * 识别单个frame
     * @param frame
     * @return
     */
    public native String vadProcessFrame(byte[] frame);


    private class AsrLiveThread extends Thread{
        @Override
        public void run() {
            Log.i(AsrLiveEngine.class.getName(), ">语音识别线程启动Threadid=" + this.getId());
            while(true){
                try {
                    PCMFile pf = AsrLiveEngine.this.asrAsyncQueen.take();
                    if(pf instanceof AsrThreadCloseFlag) {
                        asrListener.liveEnd();
                        break;
                    }
                    long start=System.currentTimeMillis();
                    String retmsg = AsrLiveEngine.this.post(
                            AsrLiveEngine.this.apiurl,
                            pf.getFileContent());
                    Log.d("识别结果:","==内容"+retmsg+"==耗时:="+(System.currentTimeMillis()-start));
                    AsrLiveEngine.this.asrListener.onResult(retmsg);
                } catch (InterruptedException e) {
                    Log.e(AsrLiveEngine.class.getName(), "语音识别队列异常", e);
                }
            }

            Log.i(AsrLiveEngine.class.getName(), "<语音识别线程结束Threadid=" + this.getId());
        }
    }

    /**
     * AsrLiveEngine构建器
     */
    public static class Builder {
        private AsrListener b_asrListener;
        private String b_apiurl;
        private int writeTimeout = 10;
        private int readTimeout = 20;

        public Builder listener(AsrListener asrListener) {
            this.b_asrListener = asrListener;
            return this;
        }

        public Builder api(String apiurl) {
            this.b_apiurl = apiurl;
            return this;
        }

        public Builder writeTimeout(int timeout){
            this.writeTimeout = timeout;
            return this;
        }

        public Builder readTimeout(int timeout){
            this.readTimeout = timeout;
            return this;
        }

        public AsrLiveEngine build() {
            AsrLiveEngine engine = new AsrLiveEngine();
            engine.asrListener = this.b_asrListener;
            engine.apiurl = this.b_apiurl;
            engine.httpClient = new OkHttpClient.Builder()
                    .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                    .readTimeout(readTimeout, TimeUnit.SECONDS)
                    .build();
            return engine;
        }

    }

    private static class AsrThreadCloseFlag extends PCMFile{}
}
