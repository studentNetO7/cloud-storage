package com.semenova.cloudstorage.dto;

public class FileResponse {

    private String filename;
    private long size;

    // Конструктор
    public FileResponse() {}

    public FileResponse(String filename, long size) {
        this.filename = filename;
        this.size = size;
    }

    // Геттеры и сеттеры
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
