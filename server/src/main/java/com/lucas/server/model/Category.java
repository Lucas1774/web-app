package com.lucas.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Category {
    @JsonProperty("CATEGORY_ID")
    private int id;
    @JsonProperty("NAME")
    private String name;
    @JsonProperty("ORDER")
    private int order;
}
