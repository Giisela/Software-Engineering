/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ua.es.kolorblind.java.kafka;

public class KafkaMessage {

   
   
    private String fileType;
    private String fileName;
    private String id;
    private double processTime;
    
     
    public KafkaMessage(){
        super();
       
    }
    
    public KafkaMessage(String fileType, String id, double processTime)
    {
        super();
        this.fileType = fileType;
        this.id = id;
        this.processTime = processTime;
    }
    
    public KafkaMessage(String fileType,  String fileName, String id )
    {
        super();
        this.fileType = fileType;
        this.fileName = fileName;
        this.id = id;

    }
   
    
   
   
    
    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public double getProcessTime() {
        return processTime;
    }

    public void setProcessTime(double processTime) {
        this.processTime = processTime;
    }
    
     @Override
    public String toString() {
        return "KafkaMessage{" + "fileType=" + fileType + ", fileName=" + fileName + ", id=" + id + '}';
    }
    
}
