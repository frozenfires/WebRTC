package com.yong.socket;

public class TestServer {

    public static void main(String args[]){

        SocketServer.getInstance().start(3307);

        //获取病历对话
        String medicalRecord=   MedicalRecordManager.getInstance().getMedicalRecord("12345");
        MedicalRecordManager.getInstance().removeMedicalRecord("12345");
        System.out.println(medicalRecord);


    }


}
