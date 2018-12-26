package com.yong.socket;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SocketServer {

    private static SocketServer socketServer=new SocketServer();

    //存放医生用户的socket连接
    private Map<String ,ServerAccept>   userSocketMap=new HashMap<String,ServerAccept>();

    public static SocketServer getInstance(){
        return socketServer;
    }


    public void start(int port)  {
            //构建服务器对象
        try {
            ServerSocket ss = new ServerSocket(port);

            System.out.println("服务器端socketServer启动..");

            while(true){

                Socket s=ss.accept();

                ServerAccept serverAccept =new ServerAccept(s,userSocketMap);
                serverAccept.start();



            }


            } catch (IOException e) {
            e.printStackTrace();
        }



        }




}


 class ServerAccept extends Thread {

    //当前用户
    private Socket s;

    private Map<String,ServerAccept> userSocketMap;


    //预约号
    private String RESERVATION_NUMBER_S=null;

    private boolean clientFlag=true;
    
    public Socket getSocket(){
    	return s;
    }
    
    
    public void setClientFlag(boolean flag){
    	this.clientFlag=flag;
    }

    public  ServerAccept(Socket s,Map userSocketMap){
        this.s = s;
        this.userSocketMap=userSocketMap;
    }

    public void run(){


        try {

        	InputStreamReader inputStreamReader=new InputStreamReader(s.getInputStream(),"UTF-8");
            //读取用户信息
            BufferedReader reader = new BufferedReader(inputStreamReader);
            
            //不断的读取写出数据
            while(clientFlag){

                //接收数据
                String info = null;

                //如果读取信息不为空
                if((info=reader.readLine()) != null){
                  //  Log.e("serverAccept",info);
                    System.out.println("服务端接收到;"+info);


                    String isClose=null; String  local_id=null; String   remote_id=null;
                    String content=null;
                    try {
                        JSONObject jsonObject = JSON.parseObject(info);;
                        isClose=jsonObject.getString("SYSTEM_SOCKET_CLOSE");
                          local_id=jsonObject.getString("LOCAL_ID");
                          remote_id=jsonObject.getString("REMOTE_ID");
                           content=jsonObject.getString("CONTENT");

                        } catch (JSONException e) {
                        System.out.println("SocketServer JSONException: "+e.getMessage());
                         // e.printStackTrace();
                        }
                    
                        //关闭
                       if("true".equals(isClose)){
                    	   if(userSocketMap.containsKey(local_id)){
                    		  ServerAccept accept=userSocketMap.get(local_id);
                      		 // Socket s_oldSocket=accept.getSocket();
                      		 // s_oldSocket.close();
                      		  accept.setClientFlag(false);
                      	 }
                        this.clientFlag=false;
                        this.userSocketMap.remove(local_id);
                        }

              
                        //注册
                         if("".equals(remote_id)){
                        	 if(userSocketMap.containsKey(local_id)){
                        		  ServerAccept accept=userSocketMap.get(local_id);
                        		  
                          		  Socket s_oldSocket=accept.getSocket();
                          		  
                          		  JSONObject jsonTempJsonObject=new JSONObject();
                          		  jsonTempJsonObject.put("SYSTEM_CLOSE_CLIENT", "true");
                          		  
                          		  sendMessage(s_oldSocket, jsonTempJsonObject.toString());
                          		  
                          		 // s_oldSocket.close();
                          		  accept.setClientFlag(false);
                        	 }
                            userSocketMap.put(local_id,this);
                            
                            JSONObject jsonDataJsonObject=new JSONObject();
                            jsonDataJsonObject.put("SYSTEM_REGISTER_CLIENT", "true");
                          
                            sendMessage(s, jsonDataJsonObject.toString());
                            
                        }else{

                            if(this.userSocketMap.containsKey(remote_id)){
                                //给远程发送信息
                            	
                            	  ServerAccept accept=userSocketMap.get(remote_id);
                          		  Socket socket=accept.getSocket();
                                 
                              	 sendMessage(socket, content);

                            }

                        }

                }


            }

        } catch (IOException e1) {
            //出现异常时，flag=false
             System.out.println("SocketServer exception:"+e1.getMessage());
             this.clientFlag=false;
          //   e1.printStackTrace();

        }


    }
    
    
    /**
     * 发送信息
     * @param socket
     * @param content
     * @throws IOException
     */
    private void sendMessage(Socket socket,String content) throws IOException{  	
           PrintWriter pw=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
           //写入信息
           pw.println(content);
           pw.flush();
    	
    }
    
    
    
    
  
}

