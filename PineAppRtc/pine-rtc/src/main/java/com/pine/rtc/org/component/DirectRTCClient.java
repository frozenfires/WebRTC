/*
 *  Copyright 2016 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.pine.rtc.org.component;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of AppRTCClient that uses direct TCP connection as the signaling channel.
 * This eliminates the need for an external server. This class does not support loopback
 * connections.
 */
public class DirectRTCClient implements AppRTCClient, TCPChannelClient.TCPChannelEvents {
    private static final String TAG = "DirectRTCClient";

    // Regex pattern used for checking if room id looks like an IP.
    public static final Pattern IP_PATTERN = Pattern.compile("("
            // IPv4
            + "((\\d+\\.){3}\\d+)|"
            // IPv6
            + "\\[((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::"
            + "(([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?)\\]|"
            + "\\[(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})\\]|"
            // IPv6 without []
            + "((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::(([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?)|"
            + "(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})|"
            // Literals
            + "localhost"
            + ")"
            // Optional port number
            + "(:(\\d+))?");
    private static final int DEFAULT_PORT = 8888;
    private final ExecutorService mExecutor;
    private final SignalingEvents mEvents;
    private TCPChannelClient mTcpClient;
    private RoomConnectionParameters mConnectionParameters;
    // All alterations of the room state should be done from inside the looper thread.
    private ConnectionState mRoomState;

    public DirectRTCClient(SignalingEvents events) {
        this.mEvents = events;

        mExecutor = Executors.newSingleThreadExecutor();
        mRoomState = ConnectionState.NEW;
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Converts a Java candidate to a JSONObject.
    private static JSONObject toJsonCandidate(final IceCandidate candidate) {
        JSONObject json = new JSONObject();
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);
        return json;
    }

    // Converts a JSON candidate to a Java object.
    private static IceCandidate toJavaCandidate(JSONObject json) throws JSONException {
        return new IceCandidate(
                json.getString("id"), json.getInt("label"), json.getString("candidate"));
    }

    /**
     * Connects to the room, roomId in connectionsParameters is required. roomId must be a valid
     * IP address matching IP_PATTERN.
     */
    @Override
    public void connectToRoom(RoomConnectionParameters connectionParameters) {
        this.mConnectionParameters = connectionParameters;

        if (mConnectionParameters.loopback) {
            reportError("Loopback connections aren't supported by DirectRTCClient.");
        }

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                connectToRoomInternal();
            }
        });
    }

    @Override
    public void disconnectFromRoom() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                disconnectFromRoomInternal();
            }
        });
    }

    /**
     * Connects to the room.
     * <p>
     * Runs on the looper thread.
     */
    private void connectToRoomInternal() {
        this.mRoomState = ConnectionState.NEW;

        String endpoint = mConnectionParameters.roomId;

        Matcher matcher = IP_PATTERN.matcher(endpoint);
        if (!matcher.matches()) {
            reportError("roomId must match IP_PATTERN for DirectRTCClient.");
            return;
        }

        String ip = matcher.group(1);
        String portStr = matcher.group(matcher.groupCount());
        int port;

        if (portStr != null) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                reportError("Invalid port number: " + portStr);
                return;
            }
        } else {
            port = DEFAULT_PORT;
        }

        mTcpClient = new TCPChannelClient(mExecutor, this, ip, port);
    }

    /**
     * Disconnects from the room.
     * <p>
     * Runs on the looper thread.
     */
    private void disconnectFromRoomInternal() {
        mRoomState = ConnectionState.CLOSED;

        if (mTcpClient != null) {
            mTcpClient.disconnect();
            mTcpClient = null;
        }
        mExecutor.shutdown();
    }

    @Override
    public void sendOfferSdp(final SessionDescription sdp) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (mRoomState != ConnectionState.CONNECTED) {
                    reportError("Sending offer SDP in non connected state.");
                    return;
                }
                JSONObject json = new JSONObject();
                jsonPut(json, "sdp", sdp.description);
                jsonPut(json, "type", "offer");
                sendMessage(json.toString());
            }
        });
    }

    @Override
    public void sendAnswerSdp(final SessionDescription sdp) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                jsonPut(json, "sdp", sdp.description);
                jsonPut(json, "type", "answer");
                sendMessage(json.toString());
            }
        });
    }

    // -------------------------------------------------------------------
    // TCPChannelClient event handlers

    @Override
    public void sendLocalIceCandidate(final IceCandidate candidate) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                jsonPut(json, "type", "candidate");
                jsonPut(json, "label", candidate.sdpMLineIndex);
                jsonPut(json, "id", candidate.sdpMid);
                jsonPut(json, "candidate", candidate.sdp);

                if (mRoomState != ConnectionState.CONNECTED) {
                    reportError("Sending ICE candidate in non connected state.");
                    return;
                }
                sendMessage(json.toString());
            }
        });
    }

    /**
     * Send removed Ice candidates to the other participant.
     */
    @Override
    public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                jsonPut(json, "type", "remove-candidates");
                JSONArray jsonArray = new JSONArray();
                for (final IceCandidate candidate : candidates) {
                    jsonArray.put(toJsonCandidate(candidate));
                }
                jsonPut(json, "candidates", jsonArray);

                if (mRoomState != ConnectionState.CONNECTED) {
                    reportError("Sending ICE candidate removals in non connected state.");
                    return;
                }
                sendMessage(json.toString());
            }
        });
    }

    /**
     * If the client is the server side, this will trigger onConnectedToRoom.
     */
    @Override
    public void onTCPConnected(boolean isServer) {
        if (isServer) {
            mRoomState = ConnectionState.CONNECTED;

            SignalingParameters parameters = new SignalingParameters(
                    // Ice servers are not needed for direct connections.
                    new LinkedList<PeerConnection.IceServer>(),
                    isServer, // Server side acts as the initiator on direct connections.
                    null, // clientId
                    null, // wssUrl
                    null, // wwsPostUrl
                    null, // offerSdp
                    null // iceCandidates
            );
            mEvents.onConnectedToRoom(parameters);
        }
    }

    @Override
    public void onTCPMessage(String msg) {
        try {
            JSONObject json = new JSONObject(msg);
            String type = json.optString("type");
            if (type.equals("candidate")) {
                mEvents.onRemoteIceCandidate(toJavaCandidate(json));
            } else if (type.equals("remove-candidates")) {
                JSONArray candidateArray = json.getJSONArray("candidates");
                IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
                for (int i = 0; i < candidateArray.length(); ++i) {
                    candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i));
                }
                mEvents.onRemoteIceCandidatesRemoved(candidates);
            } else if (type.equals("answer")) {
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
                mEvents.onRemoteDescription(sdp);
            } else if (type.equals("offer")) {
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));

                SignalingParameters parameters = new SignalingParameters(
                        // Ice servers are not needed for direct connections.
                        new LinkedList<PeerConnection.IceServer>(),
                        false, // This code will only be run on the client side. So, we are not the initiator.
                        null, // clientId
                        null, // wssUrl
                        null, // wssPostUrl
                        sdp, // offerSdp
                        null // iceCandidates
                );
                mRoomState = ConnectionState.CONNECTED;
                mEvents.onConnectedToRoom(parameters);
            } else {
                reportError("Unexpected TCP message: " + msg);
            }
        } catch (JSONException e) {
            reportError("TCP message JSON parsing error: " + e.toString());
        }
    }

    @Override
    public void onTCPError(String description) {
        reportError("TCP connection error: " + description);
    }

    @Override
    public void onTCPClose() {
        mEvents.onChannelClose();
    }

    // --------------------------------------------------------------------
    // Helper functions.
    private void reportError(final String errorMessage) {
        Log.e(TAG, errorMessage);
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (mRoomState != ConnectionState.ERROR) {
                    mRoomState = ConnectionState.ERROR;
                    mEvents.onChannelError(errorMessage);
                }
            }
        });
    }

    private void sendMessage(final String message) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mTcpClient.send(message);
            }
        });
    }

    private enum ConnectionState {NEW, CONNECTED, CLOSED, ERROR}
}
