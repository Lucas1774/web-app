package com.lucas.server.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class User {
    @JsonProperty("username")
    private String username;
    @JsonProperty("password")
    private String password;
}
