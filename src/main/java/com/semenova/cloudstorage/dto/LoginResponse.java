package com.semenova.cloudstorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginResponse {

    @JsonProperty("auth-token")  // Поле в JSON будет называться "auth-token"
    private String authToken;

    public LoginResponse() {
    }

    public LoginResponse(String authToken) {
        this.authToken = authToken;
    }

}
