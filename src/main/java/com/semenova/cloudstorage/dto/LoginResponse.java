package com.semenova.cloudstorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponse {

    @JsonProperty("auth-token")  // Поле в JSON будет называться "auth-token"
    private String authToken;

    public LoginResponse() {
    }

    public LoginResponse(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
