package com.semenova.cloudstorage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
public class FileResponse {

    @Setter
    private String filename;
    private long size;
    private String sizeFormatted;

    public FileResponse() {
    }

    public FileResponse(String filename, long size) {
        this.filename = filename;
        this.size = size;
        this.sizeFormatted = formatSize(size);
    }

    public void setSize(long size) {
        this.size = size;
        this.sizeFormatted = formatSize(size);
    }

    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            double kb = size / 1024.0;
            return String.format("%.2f КБ", kb);
        } else {
            double mb = size / (1024.0 * 1024.0);
            return String.format("%.2f МБ", mb);
        }
    }
}
