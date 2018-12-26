package com.example.a123.myapp;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.github.vesung.vadsdk.ApiCloudModule;
import com.github.vesung.vadsdk.SpeechRecognitionManager;
import com.github.vesung.vadsdk.asr.AsrListener;
import com.github.vesung.vadsdk.asr.AsrLiveEngine;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    Button button;
   TextView  textview;
    private static boolean isRecording = false ;
    private ApiCloudModule audioModule=new ApiCloudModule();
  static  TextView mDisplayLog;
    private boolean mIsPlaying = false;
    private static Handler handler=null;
    final int BUFFER_SIZE = 320;
    byte[] mBuffer=null;
    private ExecutorService mExecutorService;
    SpeechRecognitionManager speechRecognitionManager=null;
   File recordFile=null;


    public static boolean getIsRecording(){
        return isRecording;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //创建属于主线程的handler
        handler=new Handler();
        mExecutorService = Executors.newSingleThreadExecutor();
         mDisplayLog= findViewById(R.id.display_log);

        mBuffer = new byte[BUFFER_SIZE];

        int offset=mDisplayLog.getLineCount()*mDisplayLog.getLineHeight();
        if (offset > mDisplayLog.getHeight()) {
            mDisplayLog.scrollTo(0,offset - mDisplayLog.getHeight());
        }
        //(TextView)findViewById(R.id.display_log).setMovementMethod(ScrollingMovementMethod.getInstance());

         //////
         speechRecognitionManager=SpeechRecognitionManager.getInstance();
        speechRecognitionManager.setSpeechRecognitionURL("http://192.168.1.132:8080/asr/baiduresp");
        AsrListener listener=new AsrListener() {
            @Override
            public void onResult(String result) {
                Log.e("识别结果：",result);
            }

            @Override
            public void liveBegin() {

            }

            @Override
            public void liveEnd() {

            }
        };

        speechRecognitionManager.setAsrListener(listener);

        speechRecognitionManager.init();

        speechRecognitionManager.startliveByte();

        //////////
    }



    /**
     * 播放录音
     *
     * @param view
     */
    public void playrecorder(View view) {
        if (!mIsPlaying) {
            mExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    doPlay(recordFile);
                }
            });

        } else {
            Toast.makeText(this, "正在播放", Toast.LENGTH_SHORT).show();
        }
    }



    public static void setLog(String content){

        /*
        new Thread(){
            public void run(){
                handler.post(runnableUi);
            }
        }.start();

        String oldText= mDisplayLog.getText().toString();
        String newContent=content+"\n"+oldText;
        mDisplayLog.setText(newContent);
        */

    }

    /**
     @param view
     */
    public void startClick(View view){



        Thread thread = new Thread(new Runnable() {

            public void run() {

                record2();

            }

        });

        thread.start();

        findViewById(R.id.button_start).setEnabled(false) ;

        findViewById(R.id.button_stop).setEnabled(true) ;


        isRecording = true;
      //  audioModule.re_start();



    }

    public static byte[] toByteArray(short[] src) {

        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {
            dest[i * 2] = (byte) (src[i] >> 8);
            dest[i * 2 + 1] = (byte) (src[i] >> 0);
        }

        return dest;
    }


    /**
     @param view
     */
    public void stopClick(View view){


        isRecording = false ;

        findViewById(R.id.button_start).setEnabled(true) ;

        findViewById(R.id.button_stop).setEnabled(false) ;


      //  audioModule.close();

    }



    /**
     @param view
     */
    public void playClick(View view){

        //play();

        playrecorder(view);
    }




    public void play() {

        // Get the file we want toplayback.

        File file= new File(Environment.getExternalStorageDirectory().getPath()+ "/UZMap/reverseme.pcm");

        // Get the length of the audio stored in the file(16 bit so 2 bytes per short)

        // and create a short array to store the recordedaudio.

        int musicLength = (int)(file.length()/2);

        short[] music = new short[musicLength];





        try {

            // Create a DataInputStream to read the audio databack from the saved file.

            InputStream is = new FileInputStream(file);

            BufferedInputStream bis = new BufferedInputStream(is);

            DataInputStream dis = new DataInputStream(bis);



            // Read the file into the musicarray.

            int i = 0;

            while (dis.available() > 0) {

                music[i] = dis.readShort();

                i++;

            }





            // Close the input streams.

            dis.close();





            // Create a new AudioTrack object using the sameparameters as the AudioRecord

            // object used to create thefile.

            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,

                    11025,

                    AudioFormat.CHANNEL_CONFIGURATION_MONO,

                    AudioFormat.ENCODING_PCM_16BIT,

                    musicLength*2,

                    AudioTrack.MODE_STREAM);

            // Start playback

            audioTrack.play();



            // Write the music buffer to the AudioTrackobject

            audioTrack.write(music, 0, musicLength);



            audioTrack.stop() ;



        } catch (Throwable t) {

            Log.e("AudioTrack","Playback Failed"+t.getMessage());

        }


    }




    public void record() {

        Log.d("record..............","dddddddddddddddddd");

        int frequency = 11025;

        int channelConfiguration =AudioFormat.CHANNEL_CONFIGURATION_MONO;

        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

        File file= new File(Environment.getExternalStorageDirectory().getPath()+ "/UZMap/reverseme.pcm");

        Log.d("new file ..............","new file..........");

        // Delete any previousrecording.

        if (file.exists())

            file.delete();





        // Create the new file.


        try {

        file.createNewFile();

        } catch (IOException e) {

            throw new IllegalStateException("Failed to create " + file.toString()+e.getMessage());

        }



        try {

            // Create a DataOuputStream to write the audiodata into the saved file.

            OutputStream os = new FileOutputStream(file);

            BufferedOutputStream bos = new BufferedOutputStream(os);

            DataOutputStream dos = new DataOutputStream(bos);



            // Create a new AudioRecord object to record theaudio.

            int bufferSize =AudioRecord.getMinBufferSize(frequency,channelConfiguration, audioEncoding);

            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,

                    frequency, channelConfiguration,

                    audioEncoding, bufferSize);



            short[] buffer = new short[bufferSize];

            audioRecord.startRecording();



            isRecording = true ;

            while (isRecording) {

                int bufferReadResult = audioRecord.read(buffer, 0,bufferSize);

                /*
                if (AudioRecord.ERROR_INVALID_OPERATION != bufferReadResult) {
                     Log.e("put data...........","putData..........");
                     byte[] byteData=toByteArray(buffer);
                      byte[] newData=   byteData.clone();
                      ApiCloudModule.putAudioDataQueen(newData);
                }
                   */
                for (int i = 0; i < bufferReadResult;i++) {
                    short num=buffer[i];
                    dos.writeShort(num);
                  //  byte byteData[]=shortToByte(num);
                  //  InputStream sbs = new ByteArrayInputStream(byteData);
                  //  audioModule.getLiveEngine().live( sbs);
                 //  byte[] byteData = shortToByte(num);
                  // byte[] newData = byteData.clone();
                 //  ApiCloudModule.putAudioDataQueen(newData);
                 //   Log.e("put data...........", "putData..........");
                }

                }





            audioRecord.stop();

            dos.close();



        } catch (Throwable t) {

            Log.e("AudioRecord","Recording Failed"+t.getMessage());

        }

    }



    public void record2() {

        Log.d("record2..............","dddddddddddddddddd");

        recordFile= new File(Environment.getExternalStorageDirectory().getPath()+ "/UZMap/reverseme2.pcm");

        if (recordFile.exists())
            recordFile.delete();
        try {
            recordFile.createNewFile();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to create " + recordFile.toString()+e.getMessage());

        }

        AudioRecord    mAudioRecord=null;
        try {

            // Create a DataOuputStream to write the audiodata into the saved file.

            OutputStream os = new FileOutputStream(recordFile);

            int audioSource = MediaRecorder.AudioSource.MIC;
            //所有android系统都支持
            int sampleRate = 16000;
            //单声道输入
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            //PCM_16是所有android系统都支持的
            int autioFormat = AudioFormat.ENCODING_PCM_16BIT;
            //计算AudioRecord内部buffer最小
            int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, autioFormat);

            //buffer不能小于最低要求，也不能小于我们每次我们读取的大小。
                mAudioRecord = new AudioRecord(audioSource, 16000, 16, 2, 160000);

            isRecording = true ;
            mAudioRecord.startRecording();;

            //循环读取数据，写入输出流中
            while (isRecording) {
                //只要还在录音就一直读取
                int read = mAudioRecord.read(mBuffer, 0, BUFFER_SIZE);
                if(read>0){
                   byte[] newBuffer=mBuffer.clone();
                   os.write(mBuffer, 0, read);
                  // AsrLiveEngine.putAudioDataQueen(newBuffer);
                    speechRecognitionManager.putByteData(newBuffer);
                }

            }


            mAudioRecord.stop();


        } catch (Throwable t) {

            Log.e("AudioRecord","Recording Failed"+t.getMessage());

        }finally{

            if(mAudioRecord!=null){
                //mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;

            }
        }

    }



    private void doPlay(File audioFile) {
        if(audioFile !=null){
            Log.i("Tag8","go there");
            //配置播放器
            //音乐类型，扬声器播放
            int streamType= AudioManager.STREAM_MUSIC;
            //录音时采用的采样频率，所以播放时同样的采样频率
            int sampleRate=16000;
            //单声道，和录音时设置的一样
            int channelConfig=AudioFormat.CHANNEL_OUT_MONO;
            //录音时使用16bit，所以播放时同样采用该方式
            int audioFormat=AudioFormat.ENCODING_PCM_16BIT;
            //流模式
            int mode= AudioTrack.MODE_STREAM;

            //计算最小buffer大小
            int minBufferSize=AudioTrack.getMinBufferSize(sampleRate,channelConfig,audioFormat);

            //构造AudioTrack  不能小于AudioTrack的最低要求，也不能小于我们每次读的大小
            AudioTrack audioTrack=new AudioTrack(streamType,sampleRate,channelConfig,audioFormat,
                    160000,mode);

            //从文件流读数据
            FileInputStream inputStream=null;
            try{
                //循环读数据，写到播放器去播放
                inputStream=new FileInputStream(audioFile);

                //循环读数据，写到播放器去播放
                int read;
                audioTrack.play();
                //只要没读完，循环播放
                while ((read=inputStream.read(mBuffer))>0){
                    Log.i("Tag8","read:"+read);
                    int ret=audioTrack.write(mBuffer,0,read);
                    //检查write的返回值，处理错误
                    switch (ret){
                        case AudioTrack.ERROR_INVALID_OPERATION:
                        case AudioTrack.ERROR_BAD_VALUE:
                        case AudioManager.ERROR_DEAD_OBJECT:
                            //playFail();
                            return;
                        default:
                            break;
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
                //读取失败
              //  playFail();
            }finally {
                mIsPlaying=false;
                //关闭文件输入流
                if(inputStream !=null){
                    closeStream(inputStream);
                }
                //播放器释放
                resetQuietly(audioTrack);
            }

            //循环读数据，写到播放器去播放


            //错误处理，防止闪退

        }
    }


    /**
     * 关闭输入流
     * @param inputStream
     */
    private void closeStream(FileInputStream inputStream){
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetQuietly(AudioTrack audioTrack){
        try{
            audioTrack.stop();
            audioTrack.release();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * 注释：short到字节数组的转换！
     *
     * @param
     * @return
     */
    public static byte[] shortToByte(short number) {
        int temp = number;
        byte[] b = new byte[2];
        for (int i = 0; i < b.length; i++) {
            b[i] = new Integer(temp & 0xff).byteValue();//将最低位保存在最低位
                    temp = temp >> 8; // 向右移8位
        }
        return b;
    }


}
