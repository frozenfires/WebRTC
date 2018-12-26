/*
 * Project:   testSocket
 * Copyright: Copyright (c) 2014-2017
 * Company:   Xi'an YongWay Information Technology Co.,Ltd.
 */

package com.yong.socket;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @Title  </p>
 * @Description  </p>
 * @author <a href="mailto:yanggx@yongway.com">Yanggx</a> </p>
 * @Date 2018年12月6日 下午4:55:29
 */
public class TestClient {

	public static void main(String[] args) {


		 Map userMap=new HashMap<String,SocketClient>();

	    
	            SocketClient client =  SocketClient.getInstance();

	            Receivelistener receivelistener = new Receivelistener() {
	                @Override
	                public String onMessage(String receiveContent) {

	                    //    Log.e("oneMessage",receiveContent);
	                    System.out.println(" ====客户端接收=======" + receiveContent);
	                    return null;
	                }
	            };


	            client.start(receivelistener, SocketUtil.SERVER_IP, SocketUtil.SERVER_PORT);
	            
	                     
	            client.register("Patient_2");
	            
	            //client.close();
	        
	            client.send("Doctor_2"," 你好，医生2222");


	        

		
		

	}

}
