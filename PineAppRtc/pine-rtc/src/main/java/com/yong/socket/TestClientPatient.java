package com.yong.socket;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TestClientPatient {

    public static void main(String args[]) {

        Map userMap=new HashMap<String,SocketClient>();

        for (int i = 0; i < 100; i++) {
            SocketClient client = new SocketClient();// SocketClient.getInstance();

            Receivelistener receivelistener = new Receivelistener() {
                @Override
                public String onMessage(String receiveContent) {

                    //    Log.e("oneMessage",receiveContent);
                    System.out.println(" ====客户端接收=======" + receiveContent);
                    return null;
                }
            };



            //client.sendToServer(SocketUtil.ROLE_PATIENT,"patient_id1","doctor_1","你好，我是患者","12345");

           // client.close();

            client.start(receivelistener, SocketUtil.SERVER_IP, SocketUtil.SERVER_PORT);
            client.register("patient_"+i);
            //client.send("Doctor_"+i, "你好，我是患者"+i);

           userMap.put(i,client);

        }

        for(int i=0;i<100000;i++){

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Random rand = new Random();
            int num = rand.nextInt(100) ;
            SocketClient client= (SocketClient) userMap.get(num);
            client.send("Doctor_"+num,"我是患者 patient_"+num);


        }



    }


}