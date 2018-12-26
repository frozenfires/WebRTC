//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.webrtc.voiceengine;

import android.annotation.TargetApi;
import android.media.AudioRecord;
import android.os.Process;
import android.util.Log;

import com.pine.rtc.ui.activity.MyCallActivity;
import com.pine.rtc.util.BundleParamsManager;
import com.yong.socket.MedicalRecordManager;
import com.yong.socket.Receivelistener;
import com.yong.socket.SocketClient;
import com.yong.socket.SocketUtil;

import java.nio.ByteBuffer;
import org.webrtc.Logging;
import org.webrtc.ThreadUtils;

public class WebRtcAudioRecord {
    private static final boolean DEBUG = false;
    private static final String TAG = "WebRtcAudioRecord";
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CALLBACK_BUFFER_SIZE_MS = 10;
    private static final int BUFFERS_PER_SECOND = 100;
    private static final int BUFFER_SIZE_FACTOR = 2;
    private static final long AUDIO_RECORD_THREAD_JOIN_TIMEOUT_MS = 2000L;
    private static final int DEFAULT_AUDIO_SOURCE = getDefaultAudioSource();
    private static int audioSource;
    private final long nativeAudioRecord;
    private WebRtcAudioEffects effects = null;
    private ByteBuffer byteBuffer;
    private AudioRecord audioRecord = null;
    private WebRtcAudioRecord.AudioRecordThread audioThread = null;
    private static volatile boolean microphoneMute;
    private byte[] emptyBytes;
    private static WebRtcAudioRecord.WebRtcAudioRecordErrorCallback errorCallback;
    private static WebRtcAudioRecord.WebRtcAudioRecordCallback mWebRtcAudioRecordCallback;

    private static RecordTextlistener recordTextlistener;

    public static void setErrorCallback(WebRtcAudioRecord.WebRtcAudioRecordErrorCallback errorCallback) {
        Logging.d("WebRtcAudioRecord", "Set error callback");
        errorCallback = errorCallback;
    }

    public static void setWebRtcAudioRecordCallback(WebRtcAudioRecord.WebRtcAudioRecordCallback callback) {
        Logging.d("WebRtcAudioRecord", "Set record callback");
        mWebRtcAudioRecordCallback = callback;
    }

    WebRtcAudioRecord(long nativeAudioRecord) {
        Logging.d("WebRtcAudioRecord", "ctor" + WebRtcAudioUtils.getThreadInfo());
        this.nativeAudioRecord = nativeAudioRecord;
        this.effects = WebRtcAudioEffects.create();
    }

    private boolean enableBuiltInAEC(boolean enable) {
        Logging.d("WebRtcAudioRecord", "enableBuiltInAEC(" + enable + ')');
        if (this.effects == null) {
            Logging.e("WebRtcAudioRecord", "Built-in AEC is not supported on this platform");
            return false;
        } else {
            return this.effects.setAEC(enable);
        }
    }

    private boolean enableBuiltInNS(boolean enable) {
        Logging.d("WebRtcAudioRecord", "enableBuiltInNS(" + enable + ')');
        if (this.effects == null) {
            Logging.e("WebRtcAudioRecord", "Built-in NS is not supported on this platform");
            return false;
        } else {
            return this.effects.setNS(enable);
        }
    }

    private int initRecording(int sampleRate, int channels) {
        Logging.d("WebRtcAudioRecord", "initRecording(sampleRate=" + sampleRate + ", channels=" + channels + ")");
        if (this.audioRecord != null) {
            this.reportWebRtcAudioRecordInitError("InitRecording called twice without StopRecording.");
            return -1;
        } else {
            int bytesPerFrame = channels * 2;
            int framesPerBuffer = sampleRate / 100;
            this.byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * framesPerBuffer);
            Log.e("bytebuffer----",byteBuffer.capacity()+"");
            Logging.d("WebRtcAudioRecord", "byteBuffer.capacity: " + this.byteBuffer.capacity());
            this.emptyBytes = new byte[this.byteBuffer.capacity()];
            this.nativeCacheDirectBufferAddress(this.byteBuffer, this.nativeAudioRecord);
            int channelConfig = this.channelCountToConfiguration(channels);
            int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, 2);

            //连接服务器
            //connectSocketService();
            if (minBufferSize != -1 && minBufferSize != -2) {
                Logging.d("WebRtcAudioRecord", "AudioRecord.getMinBufferSize: " + minBufferSize);
                int bufferSizeInBytes = Math.max(2 * minBufferSize, this.byteBuffer.capacity());
                Logging.d("WebRtcAudioRecord", "bufferSizeInBytes: " + bufferSizeInBytes);

                try {
                    this.audioRecord = new AudioRecord(audioSource, 16000, 16, 2, bufferSizeInBytes);
                } catch (IllegalArgumentException var9) {
                    this.reportWebRtcAudioRecordInitError("AudioRecord ctor error: " + var9.getMessage());
                    this.releaseAudioResources();
                    return -1;
                }

                if (this.audioRecord != null && this.audioRecord.getState() == 1) {
                    if (this.effects != null) {
                        this.effects.enable(this.audioRecord.getAudioSessionId());
                    }

                    this.logMainParameters();
                    this.logMainParametersExtended();
                    if (mWebRtcAudioRecordCallback != null) {
                        mWebRtcAudioRecordCallback.onWebRtcAudioRecordInit(audioSource, 2, sampleRate, channels, 16, 100, bufferSizeInBytes);
                    }

                    return framesPerBuffer;
                } else {
                    this.reportWebRtcAudioRecordInitError("Failed to create a new AudioRecord instance");
                    this.releaseAudioResources();
                    return -1;
                }
            } else {
                this.reportWebRtcAudioRecordInitError("AudioRecord.getMinBufferSize failed: " + minBufferSize);
                return -1;
            }
        }


    }

    public static void setRecordTextlistener(RecordTextlistener recordTextlistener) {
        WebRtcAudioRecord.recordTextlistener = recordTextlistener;
    }

//    private void connectSocketService() {
//        SocketClient client=SocketClient.getInstance();
//        BundleParamsManager bundleParamsManager = BundleParamsManager.getInstance();
//        final String reservation_number = bundleParamsManager.getParam(BundleParamsManager.ROOM_ID);
//        final String role = bundleParamsManager.getParam(BundleParamsManager.ROLE);
//
//        Receivelistener receivelistener=new Receivelistener() {
//            @Override
//            public String onMessage(String receiveContent) {
//
//                Log.e("oneMessage",receiveContent);
//                if(receiveContent!=null)
//                    recordTextlistener.onMessage(receiveContent);
//                    MedicalRecordManager.getInstance().putMedicalRecordRow(reservation_number,"Patient",receiveContent);
//
//                return null;
//            }
//        };
//
//        String socketUrl =  bundleParamsManager.getParam(BundleParamsManager.SOCKET_URL);
//        if(socketUrl!=null){
//            client.start(receivelistener, socketUrl.split(":")[0],
//                    Integer.parseInt(socketUrl.split(":")[1]));
//        }
//        client.register(bundleParamsManager.getParam(BundleParamsManager.LOCAL_ID));
//
//    }

    private boolean startRecording() {
        Logging.d("WebRtcAudioRecord", "startRecording");
        assertTrue(this.audioRecord != null);
        assertTrue(this.audioThread == null);

        try {
            this.audioRecord.startRecording();
            if (mWebRtcAudioRecordCallback != null) {
                mWebRtcAudioRecordCallback.onWebRtcAudioRecordStart();
            }
        } catch (IllegalStateException var2) {
            this.reportWebRtcAudioRecordStartError(WebRtcAudioRecord.AudioRecordStartErrorCode.AUDIO_RECORD_START_EXCEPTION, "AudioRecord.startRecording failed: " + var2.getMessage());
            return false;
        }

        if (this.audioRecord.getRecordingState() != 3) {
            this.reportWebRtcAudioRecordStartError(WebRtcAudioRecord.AudioRecordStartErrorCode.AUDIO_RECORD_START_STATE_MISMATCH, "AudioRecord.startRecording failed - incorrect state :" + this.audioRecord.getRecordingState());
            return false;
        } else {
            this.audioThread = new WebRtcAudioRecord.AudioRecordThread("AudioRecordJavaThread");
            this.audioThread.start();
            return true;
        }
    }

    private boolean stopRecording() {
        Logging.d("WebRtcAudioRecord", "stopRecording");
        assertTrue(this.audioThread != null);
        this.audioThread.stopThread();
        if (!ThreadUtils.joinUninterruptibly(this.audioThread, 2000L)) {
            Logging.e("WebRtcAudioRecord", "Join of AudioRecordJavaThread timed out");
            WebRtcAudioUtils.logAudioState("WebRtcAudioRecord");
        }

        this.audioThread = null;
        if (this.effects != null) {
            this.effects.release();
        }

        this.releaseAudioResources();
        if (mWebRtcAudioRecordCallback != null) {
            mWebRtcAudioRecordCallback.onWebRtcAudioRecordStop();
        }

        return true;
    }

    private void logMainParameters() {
        Logging.d("WebRtcAudioRecord", "AudioRecord: session ID: " + this.audioRecord.getAudioSessionId() + ", channels: " + this.audioRecord.getChannelCount() + ", sample rate: " + this.audioRecord.getSampleRate());
    }

    @TargetApi(23)
    private void logMainParametersExtended() {
        if (WebRtcAudioUtils.runningOnMarshmallowOrHigher()) {
            Logging.d("WebRtcAudioRecord", "AudioRecord: buffer size in frames: " + this.audioRecord.getBufferSizeInFrames());
        }

    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    private int channelCountToConfiguration(int channels) {
        return channels == 1 ? 16 : 12;
    }

    private native void nativeCacheDirectBufferAddress(ByteBuffer var1, long var2);

    private native void nativeDataIsRecorded(int var1, long var2);

    public static synchronized void setAudioSource(int source) {
        Logging.w("WebRtcAudioRecord", "Audio source is changed from: " + audioSource + " to " + source);
        audioSource = source;
    }

    private static int getDefaultAudioSource() {
        return 7;
    }

    public static void setMicrophoneMute(boolean mute) {
        Logging.w("WebRtcAudioRecord", "setMicrophoneMute(" + mute + ")");
        microphoneMute = mute;
    }

    private void releaseAudioResources() {
        Logging.d("WebRtcAudioRecord", "releaseAudioResources");
        if (this.audioRecord != null) {
            this.audioRecord.release();
            this.audioRecord = null;
        }

    }

    private void reportWebRtcAudioRecordInitError(String errorMessage) {
        Logging.e("WebRtcAudioRecord", "Init recording error: " + errorMessage);
        WebRtcAudioUtils.logAudioState("WebRtcAudioRecord");
        if (errorCallback != null) {
            errorCallback.onWebRtcAudioRecordInitError(errorMessage);
        }

    }

    private void reportWebRtcAudioRecordStartError(WebRtcAudioRecord.AudioRecordStartErrorCode errorCode, String errorMessage) {
        Logging.e("WebRtcAudioRecord", "Start recording error: " + errorCode + ". " + errorMessage);
        WebRtcAudioUtils.logAudioState("WebRtcAudioRecord");
        if (errorCallback != null) {
            errorCallback.onWebRtcAudioRecordStartError(errorCode, errorMessage);
        }

    }

    private void reportWebRtcAudioRecordError(String errorMessage) {
        Logging.e("WebRtcAudioRecord", "Run-time recording error: " + errorMessage);
        WebRtcAudioUtils.logAudioState("WebRtcAudioRecord");
        if (errorCallback != null) {
            errorCallback.onWebRtcAudioRecordError(errorMessage);
        }

    }

    static {
        audioSource = DEFAULT_AUDIO_SOURCE;
        microphoneMute = false;
        errorCallback = null;
    }

    private class AudioRecordThread extends Thread {
        private volatile boolean keepAlive = true;

        public AudioRecordThread(String name) {
            super(name);
        }

        public void run() {
            Process.setThreadPriority(-19);
            Logging.d("WebRtcAudioRecord", "AudioRecordThread" + WebRtcAudioUtils.getThreadInfo());
            WebRtcAudioRecord.assertTrue(WebRtcAudioRecord.this.audioRecord.getRecordingState() == 3);
            long var1 = System.nanoTime();

            while(this.keepAlive) {
                //int bytesRead = WebRtcAudioRecord.this.audioRecord.read(WebRtcAudioRecord.this.byteBuffer, WebRtcAudioRecord.this.byteBuffer.capacity());
                int bytesRead = WebRtcAudioRecord.this.audioRecord.read(WebRtcAudioRecord.this.byteBuffer, WebRtcAudioRecord.this.byteBuffer.capacity());

                if (bytesRead == WebRtcAudioRecord.this.byteBuffer.capacity()) {
                    if (WebRtcAudioRecord.microphoneMute) {
                        WebRtcAudioRecord.this.byteBuffer.clear();
                        WebRtcAudioRecord.this.byteBuffer.put(WebRtcAudioRecord.this.emptyBytes);
                    }

                    if (this.keepAlive) {
                        WebRtcAudioRecord.this.nativeDataIsRecorded(bytesRead, WebRtcAudioRecord.this.nativeAudioRecord);
                        if (WebRtcAudioRecord.mWebRtcAudioRecordCallback != null) {
                            WebRtcAudioRecord.mWebRtcAudioRecordCallback.onWebRtcAudioRecording(WebRtcAudioRecord.this.byteBuffer, bytesRead, WebRtcAudioRecord.microphoneMute);
                        }
                    }
                } else {
                    String errorMessage = "AudioRecord.read failed: " + bytesRead;
                    Logging.e("WebRtcAudioRecord", errorMessage);
                    if (bytesRead == -3) {
                        this.keepAlive = false;
                        WebRtcAudioRecord.this.reportWebRtcAudioRecordError(errorMessage);
                    }
                }
            }

            try {
                if (WebRtcAudioRecord.this.audioRecord != null) {
                    WebRtcAudioRecord.this.audioRecord.stop();
                }
            } catch (IllegalStateException var5) {
                Logging.e("WebRtcAudioRecord", "AudioRecord.stop failed: " + var5.getMessage());
            }

        }

        public void stopThread() {
            Logging.d("WebRtcAudioRecord", "stopThread");
            this.keepAlive = false;
        }
    }

    public interface WebRtcAudioRecordCallback {
        void onWebRtcAudioRecordInit(int var1, int var2, int var3, int var4, int var5, int var6, int var7);

        void onWebRtcAudioRecordStart();

        void onWebRtcAudioRecording(ByteBuffer var1, int var2, boolean var3);

        void onWebRtcAudioRecordStop();
    }

    public interface WebRtcAudioRecordErrorCallback {
        void onWebRtcAudioRecordInitError(String var1);

        void onWebRtcAudioRecordStartError(WebRtcAudioRecord.AudioRecordStartErrorCode var1, String var2);

        void onWebRtcAudioRecordError(String var1);
    }

    public static enum AudioRecordStartErrorCode {
        AUDIO_RECORD_START_EXCEPTION,
        AUDIO_RECORD_START_STATE_MISMATCH;

        private AudioRecordStartErrorCode() {
        }
    }

    public interface RecordTextlistener{
        void onMessage(String message);
    }
}
