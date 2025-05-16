package com.semenova.cloudstorage.dto;

public class EditFileNameRequest {
    private String filename;

    public EditFileNameRequest() {
    }

    public EditFileNameRequest(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
