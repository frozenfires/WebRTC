package com.yong.socket;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 病历对话库管理
 */
public class MedicalRecordManager {

    private static MedicalRecordManager medicalRecordManager=new MedicalRecordManager();

    private  Map<String, List<Map<String,String>>> medicalMap=new HashMap<String, List<Map<String,String>>>();

    public static MedicalRecordManager getInstance(){
        return medicalRecordManager;
    }

    /**
     * 向病历库中添加一句话
     * @param reservation_number
     * @param who
     * @param txt
     */
    public void putMedicalRecordRow(String reservation_number,String who,String txt){

        if(medicalMap.containsKey(reservation_number)){
            List rowList=medicalMap.get(reservation_number);
            Map rowMap=new HashMap<String,String>();
            rowMap.put("who",who);
            rowMap.put("txt",txt);
            rowList.add(rowMap);
        }else{
            List rowList=new ArrayList<Map<String,String>>();
            Map rowMap=new HashMap<String,String>();
            rowMap.put("who",who);
            rowMap.put("txt",txt);
            rowList.add(rowMap);
            medicalMap.put(reservation_number,rowList);

        }


    }


    /**
     *  删除掉病历库中的对话信息
     * @param reservation_number 预约号
     */
    public void removeMedicalRecord(String reservation_number){
             medicalMap.remove(reservation_number);
    }


    /**
     * 根据预约号获取病历对话内容
     * @param reservation_number
     * @return
     */
    public String getMedicalRecord(String reservation_number){
        String record=null;
        List medicalList=medicalMap.get(reservation_number);
        if (medicalList!=null) {
            record = JSON.toJSONString(medicalList);
        }
        return record;
    }


}
