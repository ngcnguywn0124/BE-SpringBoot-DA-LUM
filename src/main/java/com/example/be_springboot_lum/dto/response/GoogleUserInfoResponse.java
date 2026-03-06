package com.example.be_springboot_lum.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response từ Google Userinfo Endpoint (GET https://www.googleapis.com/oauth2/v3/userinfo)
 */
@Data
public class GoogleUserInfoResponse {

    /** Google User ID (subject) */
    @JsonProperty("sub")
    private String sub;

    @JsonProperty("email")
    private String email;

    @JsonProperty("email_verified")
    private Boolean emailVerified;

    @JsonProperty("name")
    private String name;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("picture")
    private String picture;
}
