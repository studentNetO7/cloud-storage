package com.semenova.cloudstorage.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EditFileNameRequest {
    private String filename;

    public EditFileNameRequest() {
    }

    public EditFileNameRequest(String filename) {
        this.filename = filename;
    }

}
