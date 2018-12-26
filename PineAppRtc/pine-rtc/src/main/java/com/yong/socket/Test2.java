package com.yong.socket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Test2 {

    public static void main(String args[]){

       String medicalRecord=   MedicalRecordManager.getInstance().getMedicalRecord("12345");
        MedicalRecordManager.getInstance().removeMedicalRecord("12345");
       System.out.println(medicalRecord);


        /*
        String jsonContent = "{\"hello\":\"world\"}";
        try {
            //JSONObject object=new JSONObject(jsonContent);

            JSONObject m1=JSON.parseObject(jsonContent);//将json文本转化为jsonobject
        System.out.println(    m1.get("hello"));

        } catch (JSONException e) {
            e.printStackTrace();
        }


        List<Map<String,String>> medicalList=new ArrayList<Map<String,String>>();

        Map<String,String> rowMap=new HashMap<String,String>();
        Map<String,String> rowMap2=new HashMap<String,String>();

        rowMap.put("who","Doctor");
        rowMap.put("txt","请问您有什么问题");

        rowMap2.put("who","Doctor");
        rowMap2.put("txt","请问您有什么问题222222");

        medicalList.add(rowMap);
        medicalList.add(rowMap2);

      String cc=   JSON.toJSONString(medicalList);

      System.out.println(cc);
      */
    }








}
