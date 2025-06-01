package com.semenova.cloudstorage.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MessageResponse {
    private String message;

    public MessageResponse() {
    }

    public MessageResponse(String message) {
        this.message = message;
    }

}
