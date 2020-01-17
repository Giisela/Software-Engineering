package com.ua.es.kolorblind.java.Entity;


import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class MediaFile {
    private Long createDate;
    private String fullName;
    private Double fileSize;
    private Double processTime;
    private MediaStatus status;


    public MediaFile(){
        this.createDate = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        status = MediaStatus.RECEIVE;
    }


    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }


    public Long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Long createDate) {
        this.createDate = createDate;
    }

    public Double getFileSize() {
        return fileSize;
    }

    public void setFileSize(Double fileSize) {
        this.fileSize = fileSize;
    }

    public Double getProcessTime() {
        return processTime;
    }

    public void setProcessTime(Double processTime) {
        this.processTime = processTime;
    }

    public MediaStatus getStatus() {
        return status;
    }

    public void setStatus(MediaStatus status) {
        this.status = status;
    }
}


