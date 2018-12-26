package com.yong.socket;


import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TestClientDoctor {



    public static void main(String args[]){

        Map userMap=new HashMap<String,SocketClient>();

        for(int i=0;i<100;i++) {

            SocketClient client = new SocketClient();//SocketClient.getInstance();

            Receivelistener receivelistener = new Receivelistener() {
                @Override
                public String onMessage(String receiveContent) {

                    //    Log.e("oneMessage",receiveContent);
                    System.out.println(" ====客户端接收=======" + receiveContent);
                    return null;
                }
            };

            client.start(receivelistener, SocketUtil.SERVER_IP, SocketUtil.SERVER_PORT);


            client.register("Doctor_"+i);

            userMap.put(i,client);

            // client.sendToServer(SocketUtil.ROLE_DOCTOR,"doctor_id1","doctor_1","你好，我是医生","12345");

       }





    }









}
