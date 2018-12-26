/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.pine.rtc.org.component;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.vesung.vadsdk.SpeechRecognitionManager;
import com.pine.rtc.R;
import com.pine.rtc.ui.activity.VideoPlayerActivity;
import com.pine.rtc.util.BundleParamsManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Handles the initial setup where the user selects which room to join.
 */
public class ConnectActivity extends Activity {
    private static final String TAG = "ConnectActivity";

    private static final int CONNECTION_REQUEST = 1;
    private static final int REMOVE_FAVORITE_INDEX = 0;
    private static boolean mCommandLineRun = false;

    private ImageButton connectButton;
    private ImageButton addFavoriteButton;
    private EditText roomEditText;
    private ListView roomListView;
    private SharedPreferences sharedPref;
    private String mKeyPrefVideoCallEnabled;
    private String mKeyPrefScreencapture;
    private String mKeyPrefCamera2;
    private String mKeyPrefResolution;
    private String mKeyPrefFps;
    private String mKeyPrefCaptureQualitySlider;
    private String mKeyPrefVideoBitrateType;
    private String mKeyPrefVideoBitrateValue;
    private String mKeyPrefVideoCodec;
    private String mKeyPrefAudioBitrateType;
    private String mKeyPrefAudioBitrateValue;
    private String mKeyPrefAudioCodec;
    private String mKeyPrefHwCodecAcceleration;
    private String mKeyPrefCaptureToTexture;
    private String mKeyPrefFlexfec;
    private String mKeyPrefNoAudioProcessingPipeline;
    private String mKeyPrefAecDump;
    private String mKeyPrefOpenSLES;
    private String mKeyPrefDisableBuiltInAec;
    private String mKeyPrefDisableBuiltInAgc;
    private String mKeyPrefDisableBuiltInNs;
    private String mKeyPrefEnableLevelControl;
    private String mKeyPrefDisableWebRtcAGCAndHPF;
    private String mKeyPrefDisplayHud;
    private String mKeyPrefTracing;
    private String mKeyPrefRoomServerUrl;
    private final AdapterView.OnItemClickListener mRoomListClickListener =
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String roomId = ((TextView) view).getText().toString();
                    connectToRoom(roomId, false, false, false, 0);
                }
            };
    private final OnClickListener mConnectListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            connectToRoom(roomEditText.getText().toString(), false, false, false, 0);
        }
    };
    private String mKeyPrefRoom;
    private String mKeyPrefRoomList;
    private ArrayList<String> mRoomList;
    private ArrayAdapter<String> mAdapter;
    private final OnClickListener mAddFavoriteListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            String newRoom = roomEditText.getText().toString();
            if (newRoom.length() > 0 && !mRoomList.contains(newRoom)) {
                mAdapter.add(newRoom);
                mAdapter.notifyDataSetChanged();
            }
        }
    };
    private String mKeyPrefEnableDataChannel;
    private String mKeyPrefOrdered;
    private String mKeyPrefMaxRetransmitTimeMs;
    private String mKeyPrefMaxRetransmits;
    private String mKeyPrefDataProtocol;
    private String mKeyPrefNegotiated;
    private String mKeyPrefDataId;
    private BundleParamsManager bundleParamsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //申请权限
        if (Build.VERSION.SDK_INT >= 23) {//6.0才用动态权限
            checkPermission();
        }
        // Get setting keys.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mKeyPrefVideoCallEnabled = getString(R.string.pref_videocall_key);
        mKeyPrefScreencapture = getString(R.string.pref_screencapture_key);
        mKeyPrefCamera2 = getString(R.string.pref_camera2_key);
        mKeyPrefResolution = getString(R.string.pref_resolution_key);
        mKeyPrefFps = getString(R.string.pref_fps_key);
        mKeyPrefCaptureQualitySlider = getString(R.string.pref_capturequalityslider_key);
        mKeyPrefVideoBitrateType = getString(R.string.pref_maxvideobitrate_key);
        mKeyPrefVideoBitrateValue = getString(R.string.pref_maxvideobitratevalue_key);
        mKeyPrefVideoCodec = getString(R.string.pref_videocodec_key);
        mKeyPrefHwCodecAcceleration = getString(R.string.pref_hwcodec_key);
        mKeyPrefCaptureToTexture = getString(R.string.pref_capturetotexture_key);
        mKeyPrefFlexfec = getString(R.string.pref_flexfec_key);
        mKeyPrefAudioBitrateType = getString(R.string.pref_startaudiobitrate_key);
        mKeyPrefAudioBitrateValue = getString(R.string.pref_startaudiobitratevalue_key);
        mKeyPrefAudioCodec = getString(R.string.pref_audiocodec_key);
        mKeyPrefNoAudioProcessingPipeline = getString(R.string.pref_noaudioprocessing_key);
        mKeyPrefAecDump = getString(R.string.pref_aecdump_key);
        mKeyPrefOpenSLES = getString(R.string.pref_opensles_key);
        mKeyPrefDisableBuiltInAec = getString(R.string.pref_disable_built_in_aec_key);
        mKeyPrefDisableBuiltInAgc = getString(R.string.pref_disable_built_in_agc_key);
        mKeyPrefDisableBuiltInNs = getString(R.string.pref_disable_built_in_ns_key);
        mKeyPrefEnableLevelControl = getString(R.string.pref_enable_level_control_key);
        mKeyPrefDisableWebRtcAGCAndHPF = getString(R.string.pref_disable_webrtc_agc_and_hpf_key);
        mKeyPrefDisplayHud = getString(R.string.pref_displayhud_key);
        mKeyPrefTracing = getString(R.string.pref_tracing_key);
        mKeyPrefRoomServerUrl = getString(R.string.pref_room_server_url_key);
        mKeyPrefRoom = getString(R.string.pref_room_key);
        mKeyPrefRoomList = getString(R.string.pref_room_list_key);
        mKeyPrefEnableDataChannel = getString(R.string.pref_enable_datachannel_key);
        mKeyPrefOrdered = getString(R.string.pref_ordered_key);
        mKeyPrefMaxRetransmitTimeMs = getString(R.string.pref_max_retransmit_time_ms_key);
        mKeyPrefMaxRetransmits = getString(R.string.pref_max_retransmits_key);
        mKeyPrefDataProtocol = getString(R.string.pref_data_protocol_key);
        mKeyPrefNegotiated = getString(R.string.pref_negotiated_key);
        mKeyPrefDataId = getString(R.string.pref_data_id_key);

        setContentView(R.layout.activity_connect);

        roomEditText = (EditText) findViewById(R.id.room_edittext);
        roomEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    addFavoriteButton.performClick();
                    return true;
                }
                return false;
            }
        });
        roomEditText.requestFocus();

        roomListView = (ListView) findViewById(R.id.room_listview);
        roomListView.setEmptyView(findViewById(android.R.id.empty));
        roomListView.setOnItemClickListener(mRoomListClickListener);
        registerForContextMenu(roomListView);
        connectButton = (ImageButton) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(mConnectListener);
        addFavoriteButton = (ImageButton) findViewById(R.id.add_favorite_button);
        addFavoriteButton.setOnClickListener(mAddFavoriteListener);

        // If an implicit VIEW intent is launching the app, go directly to that URL.
        final Intent intent = getIntent();
        if ("android.intent.action.VIEW".equals(intent.getAction()) && !mCommandLineRun) {
            boolean loopback = intent.getBooleanExtra(CallActivity.EXTRA_LOOPBACK, false);
            int runTimeMs = intent.getIntExtra(CallActivity.EXTRA_RUNTIME, 0);
            boolean useValuesFromIntent =
                    intent.getBooleanExtra(CallActivity.EXTRA_USE_VALUES_FROM_INTENT, false);
            String room = sharedPref.getString(mKeyPrefRoom, "");
            connectToRoom(room, true, loopback, useValuesFromIntent, runTimeMs);
        }

        findViewById(R.id.play_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ConnectActivity.this, VideoPlayerActivity.class));
            }
        });

        Bundle bundle = this.getIntent().getExtras();
//        Bundle bundle = new Bundle();
//        bundle.putString("roomId","3333");
//        bundle.putString("roomUrl","http://119.23.39.109");
//        bundle.putString("speechUrl","http://120.78.95.157:7080/asr/baiduresp");
//        bundle.putString("role","Doctor");
//        bundle.putString("socketUrl","120.78.95.157:3307");
//        bundle.putString("remoteId","1111");
//        bundle.putString("localId","3333");
//        bundle.putString("packageName","3333");
//        bundle.putString("commType","02");


        setBundleParams(bundle);

        String roomId = bundleParamsManager.getParam(BundleParamsManager.ROOM_ID);
        Log.d("roomID-----",roomId);
        if(roomId!=null&&!"".equals(roomId)){
            //设置语音识别服务器地址
            String speechUrl = bundleParamsManager.getParam(BundleParamsManager.SPEECH_URL);
            SpeechRecognitionManager speechRecognitionManager= SpeechRecognitionManager.getInstance();
            speechRecognitionManager.setSpeechRecognitionURL(speechUrl);
            Log.d("语音识别url",speechUrl);
            connectToRoom(roomId, false, false, false, 0);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.connect_menu, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.room_listview) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(mRoomList.get(info.position));
            String[] menuItems = getResources().getStringArray(R.array.roomListContextMenu);
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        } else {
            super.onCreateContextMenu(menu, v, menuInfo);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == REMOVE_FAVORITE_INDEX) {
            AdapterView.AdapterContextMenuInfo info =
                    (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            mRoomList.remove(info.position);
            mAdapter.notifyDataSetChanged();
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items.
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_loopback) {
            connectToRoom(null, false, true, false, 0);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        String room = roomEditText.getText().toString();
        String roomListJson = new JSONArray(mRoomList).toString();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(mKeyPrefRoom, room);
        editor.putString(mKeyPrefRoomList, roomListJson);
        editor.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        String room = sharedPref.getString(mKeyPrefRoom, "");
        roomEditText.setText(room);
        mRoomList = new ArrayList<String>();
        String roomListJson = sharedPref.getString(mKeyPrefRoomList, null);
        if (roomListJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(roomListJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    mRoomList.add(jsonArray.get(i).toString());
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to load room list: " + e.toString());
            }
        }
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mRoomList);
        roomListView.setAdapter(mAdapter);
        if (mAdapter.getCount() > 0) {
            roomListView.requestFocus();
            roomListView.setItemChecked(0, true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONNECTION_REQUEST && mCommandLineRun) {
            Log.d(TAG, "Return: " + resultCode);
            setResult(resultCode);
            mCommandLineRun = false;
            finish();
        }
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private String sharedPrefGetString(
            int attributeId, String intentName, int defaultId, boolean useFromIntent) {
        String defaultValue = getString(defaultId);
        if (useFromIntent) {
            String value = getIntent().getStringExtra(intentName);
            if (value != null) {
                return value;
            }
            return defaultValue;
        } else {
            String attributeName = getString(attributeId);
            return sharedPref.getString(attributeName, defaultValue);
        }
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private boolean sharedPrefGetBoolean(
            int attributeId, String intentName, int defaultId, boolean useFromIntent) {
        boolean defaultValue = Boolean.valueOf(getString(defaultId));
        if (useFromIntent) {
            return getIntent().getBooleanExtra(intentName, defaultValue);
        } else {
            String attributeName = getString(attributeId);
            return sharedPref.getBoolean(attributeName, defaultValue);
        }
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private int sharedPrefGetInteger(
            int attributeId, String intentName, int defaultId, boolean useFromIntent) {
        String defaultString = getString(defaultId);
        int defaultValue = Integer.parseInt(defaultString);
        if (useFromIntent) {
            return getIntent().getIntExtra(intentName, defaultValue);
        } else {
            String attributeName = getString(attributeId);
            String value = sharedPref.getString(attributeName, defaultString);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Wrong setting for: " + attributeName + ":" + value);
                return defaultValue;
            }
        }
    }

    private void connectToRoom(String roomId, boolean commandLineRun, boolean loopback,
                               boolean useValuesFromIntent, int runTimeMs) {
        this.mCommandLineRun = commandLineRun;

        // roomId is random for loopback.
        if (loopback) {
            roomId = Integer.toString((new Random()).nextInt(100000000));
        }

//        this.getIntent().getStringExtra("roomUrl");
        String roomUrl = bundleParamsManager.getParam(BundleParamsManager.ROOM_URL);
        //String roomUrl = this.getIntent().getStringExtra("roomUrl");

        // Video call enabled flag.
        boolean videoCallEnabled = sharedPrefGetBoolean(R.string.pref_videocall_key,
                CallActivity.EXTRA_VIDEO_CALL, R.string.pref_videocall_default, useValuesFromIntent);
        String commType = bundleParamsManager.getParam(BundleParamsManager.COMM_TYPE);
        if("01".equals(commType)){
            videoCallEnabled = false;
        }else if("02".equals(commType)){
            videoCallEnabled = true;
        }


        // Use screencapture option.
        boolean useScreencapture = sharedPrefGetBoolean(R.string.pref_screencapture_key,
                CallActivity.EXTRA_SCREENCAPTURE, R.string.pref_screencapture_default, useValuesFromIntent);

        // Use Camera2 option.
        boolean useCamera2 = sharedPrefGetBoolean(R.string.pref_camera2_key, CallActivity.EXTRA_CAMERA2,
                R.string.pref_camera2_default, useValuesFromIntent);

        // Get default codecs.
        String videoCodec = sharedPrefGetString(R.string.pref_videocodec_key,
                CallActivity.EXTRA_VIDEOCODEC, R.string.pref_videocodec_default, useValuesFromIntent);
        String audioCodec = sharedPrefGetString(R.string.pref_audiocodec_key,
                CallActivity.EXTRA_AUDIOCODEC, R.string.pref_audiocodec_default, useValuesFromIntent);

        // Check HW codec flag.
        boolean hwCodec = sharedPrefGetBoolean(R.string.pref_hwcodec_key,
                CallActivity.EXTRA_HWCODEC_ENABLED, R.string.pref_hwcodec_default, useValuesFromIntent);

        // Check Capture to texture.
        boolean captureToTexture = sharedPrefGetBoolean(R.string.pref_capturetotexture_key,
                CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, R.string.pref_capturetotexture_default,
                useValuesFromIntent);

        // Check FlexFEC.
        boolean flexfecEnabled = sharedPrefGetBoolean(R.string.pref_flexfec_key,
                CallActivity.EXTRA_FLEXFEC_ENABLED, R.string.pref_flexfec_default, useValuesFromIntent);

        // Check Disable Audio Processing flag.
        boolean noAudioProcessing = sharedPrefGetBoolean(R.string.pref_noaudioprocessing_key,
                CallActivity.EXTRA_NOAUDIOPROCESSING_ENABLED, R.string.pref_noaudioprocessing_default,
                useValuesFromIntent);

        // Check Disable Audio Processing flag.
        boolean aecDump = sharedPrefGetBoolean(R.string.pref_aecdump_key,
                CallActivity.EXTRA_AECDUMP_ENABLED, R.string.pref_aecdump_default, useValuesFromIntent);

        // Check OpenSL ES enabled flag.
        boolean useOpenSLES = sharedPrefGetBoolean(R.string.pref_opensles_key,
                CallActivity.EXTRA_OPENSLES_ENABLED, R.string.pref_opensles_default, useValuesFromIntent);

        // Check Disable built-in AEC flag.
        boolean disableBuiltInAEC = sharedPrefGetBoolean(R.string.pref_disable_built_in_aec_key,
                CallActivity.EXTRA_DISABLE_BUILT_IN_AEC, R.string.pref_disable_built_in_aec_default,
                useValuesFromIntent);

        // Check Disable built-in AGC flag.
        boolean disableBuiltInAGC = sharedPrefGetBoolean(R.string.pref_disable_built_in_agc_key,
                CallActivity.EXTRA_DISABLE_BUILT_IN_AGC, R.string.pref_disable_built_in_agc_default,
                useValuesFromIntent);

        // Check Disable built-in NS flag.
        boolean disableBuiltInNS = sharedPrefGetBoolean(R.string.pref_disable_built_in_ns_key,
                CallActivity.EXTRA_DISABLE_BUILT_IN_NS, R.string.pref_disable_built_in_ns_default,
                useValuesFromIntent);

        // Check Enable level control.
        boolean enableLevelControl = sharedPrefGetBoolean(R.string.pref_enable_level_control_key,
                CallActivity.EXTRA_ENABLE_LEVEL_CONTROL, R.string.pref_enable_level_control_key,
                useValuesFromIntent);

        // Check Disable gain control
        boolean disableWebRtcAGCAndHPF = sharedPrefGetBoolean(
                R.string.pref_disable_webrtc_agc_and_hpf_key, CallActivity.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF,
                R.string.pref_disable_webrtc_agc_and_hpf_key, useValuesFromIntent);

        // Get video resolution from settings.
        int videoWidth = 0;
        int videoHeight = 0;
        if (useValuesFromIntent) {
            videoWidth = getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_WIDTH, 0);
            videoHeight = getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_HEIGHT, 0);
        }
        if (videoWidth == 0 && videoHeight == 0) {
//            String resolution =
//                    sharedPref.getString(mKeyPrefResolution, getString(R.string.pref_resolution_default));
            String resolution = "640 x 480";
            String[] dimensions = resolution.split("[ x]+");
            if (dimensions.length == 2) {
                try {
                    videoWidth = Integer.parseInt(dimensions[0]);
                    videoHeight = Integer.parseInt(dimensions[1]);
                } catch (NumberFormatException e) {
                    videoWidth = 0;
                    videoHeight = 0;
                    Log.e(TAG, "Wrong video resolution setting: " + resolution);
                }
            }
        }

        // Get camera fps from settings.
        int cameraFps = 0;
        if (useValuesFromIntent) {
            cameraFps = getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_FPS, 0);
        }
        if (cameraFps == 0) {
            String fps = sharedPref.getString(mKeyPrefFps, getString(R.string.pref_fps_default));
            String[] fpsValues = fps.split("[ x]+");
            if (fpsValues.length == 2) {
                try {
                    cameraFps = Integer.parseInt(fpsValues[0]);
                } catch (NumberFormatException e) {
                    cameraFps = 0;
                    Log.e(TAG, "Wrong camera fps setting: " + fps);
                }
            }
        }

        // Check capture quality slider flag.
        boolean captureQualitySlider = sharedPrefGetBoolean(R.string.pref_capturequalityslider_key,
                CallActivity.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED,
                R.string.pref_capturequalityslider_default, useValuesFromIntent);

        // Get video and audio start bitrate.
        int videoStartBitrate = 0;
        if (useValuesFromIntent) {
            videoStartBitrate = getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_BITRATE, 0);
        }
        if (videoStartBitrate == 0) {
            String bitrateTypeDefault = getString(R.string.pref_maxvideobitrate_default);
            String bitrateType = sharedPref.getString(mKeyPrefVideoBitrateType, bitrateTypeDefault);
            if (!bitrateType.equals(bitrateTypeDefault)) {
                String bitrateValue = sharedPref.getString(
                        mKeyPrefVideoBitrateValue, getString(R.string.pref_maxvideobitratevalue_default));
                videoStartBitrate = Integer.parseInt(bitrateValue);
            }
        }

        int audioStartBitrate = 0;
        if (useValuesFromIntent) {
            audioStartBitrate = getIntent().getIntExtra(CallActivity.EXTRA_AUDIO_BITRATE, 0);
        }
        if (audioStartBitrate == 0) {
            String bitrateTypeDefault = getString(R.string.pref_startaudiobitrate_default);
            String bitrateType = sharedPref.getString(mKeyPrefAudioBitrateType, bitrateTypeDefault);
            if (!bitrateType.equals(bitrateTypeDefault)) {
                String bitrateValue = sharedPref.getString(
                        mKeyPrefAudioBitrateValue, getString(R.string.pref_startaudiobitratevalue_default));
                audioStartBitrate = Integer.parseInt(bitrateValue);
            }
        }

        // Check statistics display option.
        boolean displayHud = sharedPrefGetBoolean(R.string.pref_displayhud_key,
                CallActivity.EXTRA_DISPLAY_HUD, R.string.pref_displayhud_default, useValuesFromIntent);

        boolean tracing = sharedPrefGetBoolean(R.string.pref_tracing_key, CallActivity.EXTRA_TRACING,
                R.string.pref_tracing_default, useValuesFromIntent);

        // Get datachannel options
        boolean dataChannelEnabled = sharedPrefGetBoolean(R.string.pref_enable_datachannel_key,
                CallActivity.EXTRA_DATA_CHANNEL_ENABLED, R.string.pref_enable_datachannel_default,
                useValuesFromIntent);
        boolean ordered = sharedPrefGetBoolean(R.string.pref_ordered_key, CallActivity.EXTRA_ORDERED,
                R.string.pref_ordered_default, useValuesFromIntent);
        boolean negotiated = sharedPrefGetBoolean(R.string.pref_negotiated_key,
                CallActivity.EXTRA_NEGOTIATED, R.string.pref_negotiated_default, useValuesFromIntent);
        int maxRetrMs = sharedPrefGetInteger(R.string.pref_max_retransmit_time_ms_key,
                CallActivity.EXTRA_MAX_RETRANSMITS_MS, R.string.pref_max_retransmit_time_ms_default,
                useValuesFromIntent);
        int maxRetr =
                sharedPrefGetInteger(R.string.pref_max_retransmits_key, CallActivity.EXTRA_MAX_RETRANSMITS,
                        R.string.pref_max_retransmits_default, useValuesFromIntent);
        int id = sharedPrefGetInteger(R.string.pref_data_id_key, CallActivity.EXTRA_ID,
                R.string.pref_data_id_default, useValuesFromIntent);
        String protocol = sharedPrefGetString(R.string.pref_data_protocol_key,
                CallActivity.EXTRA_PROTOCOL, R.string.pref_data_protocol_default, useValuesFromIntent);

        // Start AppRTCMobile activity.
        Log.d(TAG, "Connecting to room " + roomId + " at URL " + roomUrl);
        if (validateUrl(roomUrl)) {
            Uri uri = Uri.parse(roomUrl);
//            Intent intent = new Intent(this, CallActivity.class);
            Intent intent = new Intent(this, com.pine.rtc.ui.activity.MyCallActivity.class);
//            Intent intent = new Intent(this, com.pine.rtc.ui.activity.InterViewActivity.class);
            intent.setData(uri);
            intent.putExtra(CallActivity.EXTRA_ROOMID, roomId);
            intent.putExtra(CallActivity.EXTRA_LOOPBACK, loopback);
            intent.putExtra(CallActivity.EXTRA_VIDEO_CALL, videoCallEnabled);
            intent.putExtra(CallActivity.EXTRA_SCREENCAPTURE, useScreencapture);
            intent.putExtra(CallActivity.EXTRA_CAMERA2, useCamera2);
            intent.putExtra(CallActivity.EXTRA_VIDEO_WIDTH, videoWidth);
            intent.putExtra(CallActivity.EXTRA_VIDEO_HEIGHT, videoHeight);
            intent.putExtra(CallActivity.EXTRA_VIDEO_FPS, cameraFps);
            intent.putExtra(CallActivity.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED, captureQualitySlider);
            intent.putExtra(CallActivity.EXTRA_VIDEO_BITRATE, videoStartBitrate);
            intent.putExtra(CallActivity.EXTRA_VIDEOCODEC, videoCodec);
            intent.putExtra(CallActivity.EXTRA_HWCODEC_ENABLED, hwCodec);
            intent.putExtra(CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, captureToTexture);
            intent.putExtra(CallActivity.EXTRA_FLEXFEC_ENABLED, flexfecEnabled);
            intent.putExtra(CallActivity.EXTRA_NOAUDIOPROCESSING_ENABLED, noAudioProcessing);
            intent.putExtra(CallActivity.EXTRA_AECDUMP_ENABLED, aecDump);
            intent.putExtra(CallActivity.EXTRA_OPENSLES_ENABLED, useOpenSLES);
            intent.putExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_AEC, disableBuiltInAEC);
            intent.putExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_AGC, disableBuiltInAGC);
            intent.putExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_NS, disableBuiltInNS);
            intent.putExtra(CallActivity.EXTRA_ENABLE_LEVEL_CONTROL, enableLevelControl);
            intent.putExtra(CallActivity.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, disableWebRtcAGCAndHPF);
            intent.putExtra(CallActivity.EXTRA_AUDIO_BITRATE, audioStartBitrate);
            intent.putExtra(CallActivity.EXTRA_AUDIOCODEC, audioCodec);
            intent.putExtra(CallActivity.EXTRA_DISPLAY_HUD, displayHud);
            intent.putExtra(CallActivity.EXTRA_TRACING, tracing);
            intent.putExtra(CallActivity.EXTRA_CMDLINE, commandLineRun);
            intent.putExtra(CallActivity.EXTRA_RUNTIME, runTimeMs);

            intent.putExtra(CallActivity.EXTRA_DATA_CHANNEL_ENABLED, dataChannelEnabled);

            if (dataChannelEnabled) {
                intent.putExtra(CallActivity.EXTRA_ORDERED, ordered);
                intent.putExtra(CallActivity.EXTRA_MAX_RETRANSMITS_MS, maxRetrMs);
                intent.putExtra(CallActivity.EXTRA_MAX_RETRANSMITS, maxRetr);
                intent.putExtra(CallActivity.EXTRA_PROTOCOL, protocol);
                intent.putExtra(CallActivity.EXTRA_NEGOTIATED, negotiated);
                intent.putExtra(CallActivity.EXTRA_ID, id);
            }

            if (useValuesFromIntent) {
                if (getIntent().hasExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA)) {
                    String videoFileAsCamera =
                            getIntent().getStringExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA);
                    intent.putExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA, videoFileAsCamera);
                }

                if (getIntent().hasExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE)) {
                    String saveRemoteVideoToFile =
                            getIntent().getStringExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE);
                    intent.putExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE, saveRemoteVideoToFile);
                }

                if (getIntent().hasExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH)) {
                    int videoOutWidth =
                            getIntent().getIntExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, 0);
                    intent.putExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, videoOutWidth);
                }

                if (getIntent().hasExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT)) {
                    int videoOutHeight =
                            getIntent().getIntExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, 0);
                    intent.putExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, videoOutHeight);
                }
            }

            startActivityForResult(intent, CONNECTION_REQUEST);
            this.finish();
        }
    }

    private boolean validateUrl(String url) {
        if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
            return true;
        }

        new AlertDialog.Builder(this)
                .setTitle(getText(R.string.invalid_url_title))
                .setMessage(getString(R.string.invalid_url_text, url))
                .setCancelable(false)
                .setNeutralButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                .create()
                .show();
        return false;
    }

    public void setBundleParams(Bundle bundleParams) {
        bundleParamsManager = BundleParamsManager.getInstance();
        bundleParamsManager.addParams(BundleParamsManager.ROOM_ID,bundleParams.getString(BundleParamsManager.ROOM_ID))
                .addParams(BundleParamsManager.SPEECH_URL,bundleParams.getString(BundleParamsManager.SPEECH_URL))
                .addParams(BundleParamsManager.ROOM_URL,bundleParams.getString(BundleParamsManager.ROOM_URL))
                .addParams(BundleParamsManager.ROLE,bundleParams.getString(BundleParamsManager.ROLE))
                .addParams(BundleParamsManager.COMM_TYPE,bundleParams.getString(BundleParamsManager.COMM_TYPE))
                .addParams(BundleParamsManager.SOCKET_URL,bundleParams.getString(BundleParamsManager.SOCKET_URL))
                //.addParams(BundleParamsManager.REMOTE_ID,bundleParams.getString(BundleParamsManager.REMOTE_ID))
                .addParams(BundleParamsManager.LOCAL_ID,bundleParams.getString(BundleParamsManager.LOCAL_ID))
                .addParams(BundleParamsManager.PACKAGE_NAME,bundleParams.getString(BundleParamsManager.PACKAGE_NAME));
        if(bundleParams.getString(BundleParamsManager.REMOTE_ID)!=null){
            bundleParamsManager.addParams(BundleParamsManager.REMOTE_ID,bundleParams.getString(BundleParamsManager.REMOTE_ID));
        }


    }

    //1、首先声明一个数组permissions，将需要的权限都放在里面
    String []permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //2、创建一个mPermissionList，逐个判断哪些权限未授予，未授予的权限存储到mPerrrmissionList中
    List<String> xPermissionList = new ArrayList<>();
    private final int mRequestCode = 100;//权限请求码

    //权限判断和申请
    public void checkPermission(){
        xPermissionList.clear();//清空没有通过的权限
        //逐个判断你要的权限是否已经通过
        for(int i = 0 ;i<permissions.length;i++){
            if(ContextCompat.checkSelfPermission(this,permissions[i])!= PackageManager.PERMISSION_GRANTED){
                xPermissionList.add(permissions[i]);
            }
        }
        //申请权限
        if (xPermissionList.size() > 0) {//有权限没有通过，需要申请
            ActivityCompat.requestPermissions(this, permissions, mRequestCode);
        }
    }

    //请求权限后回调的方法
    //参数： requestCode  是我们自己定义的权限请求码
    //参数： permissions  是我们请求的权限名称数组
    //参数： grantResults 是我们在弹出页面后是否允许权限的标识数组，数组的长度对应的是权限名称数组的长度，数组的数据0表示允许权限，-1表示我们点击了禁止权限

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;//有权限没有通过
        if(requestCode == mRequestCode){
            for(int i=0;i<grantResults.length;i++){
                if(grantResults[i] == -1){
                    hasPermissionDismiss = true;
                }
            }
            //如果有权限没有被允许
            if (hasPermissionDismiss) {
                //showPermissionDialog();//跳转到系统设置权限页面，或者直接关闭页面，不让他继续访问
                //Toast.makeText(this,"权限需要申请",Toast.LENGTH_LONG).show();
            }else{
                //全部权限通过，可以进行下一步操作。。。

            }
        }
    }

    /**
     * 不再提示权限时的展示对话框
     */
    AlertDialog mPermissionDialog;
    String mPackName = "com.pine.rtc";

    private void showPermissionDialog() {
        if (mPermissionDialog == null) {
            mPermissionDialog = new AlertDialog.Builder(this)
                    .setMessage("已禁用权限，请手动授予")
                    .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelPermissionDialog();

                            Uri packageURI = Uri.parse("package:" + mPackName);
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //关闭页面或者做其他操作
                            cancelPermissionDialog();

                        }
                    })
                    .create();
        }
        mPermissionDialog.show();
    }

    //关闭对话框
    private void cancelPermissionDialog() {
        mPermissionDialog.cancel();
    }
}
