package com.lucas.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class User {
    @JsonProperty("USERNAME")
    private String username;
    @JsonProperty("PASSWORD")
    private String password;
}
