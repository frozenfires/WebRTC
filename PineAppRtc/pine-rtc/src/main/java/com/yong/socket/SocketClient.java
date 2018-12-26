package com.yong.socket;



import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {

    private static SocketClient socketClient=new SocketClient();

    private Receivelistener receivelistener=null;

    private Socket s =null;

    private String localID=null;

    public static SocketClient getInstance(){
        return socketClient;
    }

    private  ClientReceive clientReceive=null;

    public void setReceivelistener(Receivelistener receivelistener){
        this.receivelistener=receivelistener;
    }



    public void start(Receivelistener receivelistener,String ip,int port){
        //Log.d("SocketClient","客户端启动..");
        System.out.println("SocketClient 客户端启动..");
        setReceivelistener(receivelistener);
        //构建客户端对象
        try {
            s = new Socket(ip,port);
        } catch (IOException e) {
            e.printStackTrace();
        }

         clientReceive=new ClientReceive(s,receivelistener);
        clientReceive.start();

    }



    public void register(String localID){
        this.localID=localID;
        send("","");
    }





    public void send(String remoteID,String content){

        JSONObject jsondData = new JSONObject();
        try {
            jsondData.put("LOCAL_ID",localID);
            jsondData.put("REMOTE_ID",remoteID);
            jsondData.put("CONTENT",content);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        sendMessage(jsondData.toString());

    }


    private void sendMessage(String message){

        //Log.d("SocketClient send:",jsonData);
        //写入到 Socket 中
        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream(),"UTF-8"));
            //写出
            pw.println(message);
            pw.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("客户端发送;"+message);

    }


    private void sendToServerClose(){

        JSONObject jsondData = new JSONObject();
        try {
            jsondData.put("LOCAL_ID",localID);
            jsondData.put("SYSTEM_SOCKET_CLOSE","true");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendMessage(jsondData.toString());
    }



    public void close(){
        if(s!=null&&!s.isClosed()){
            sendToServerClose();
            this.clientReceive.setClientFlag(false);

        }
         
    }



}


/**
 * 客户端启动一个线程，用来接收服务器端发来的信息
 */
class ClientReceive extends Thread{

    private Socket s;

    private Receivelistener listener;
    
    private boolean  clientFlag=true;


    public ClientReceive(Socket s,Receivelistener receivelistener) {
        this.s = s;
        this.listener=receivelistener;
    }
    
    public void setClientFlag(boolean flag){
    	this.clientFlag=flag;
    }

    @Override
    public void run() {
        try {

            //构建输入流
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(),"UTF-8"));

            //不断的接收信息
            while(clientFlag){

                String info = null;

                //接收信息
                if((info=br.readLine()) != null){
                	
                	   try{
                	   JSONObject jsonObject = JSON.parseObject(info);;
                	    String client_close= (String) jsonObject.get("SYSTEM_CLOSE_CLIENT");
                	    String client_register=(String) jsonObject.get("SYSTEM_REGISTER_CLIENT");
                	      if("true".equals(client_close)){
                	    	  this.clientFlag=false;
                              continue;
                	      }
                	      if("true".equals(client_register)){
                	    	 System.out.println("register success");
                	    	 continue;
                	      }
                	   }catch(Exception e){
                		   System.out.println("ClientReceive JSONObject exception");
                	   }

                   // System.out.println(info);
                    listener.onMessage(info);
                }

            }


        } catch (IOException e) {
        	this.clientFlag=false;
            e.printStackTrace();
        }
    }
}
