package com.lucas.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class Category implements Sortable {
    @JsonProperty("ID")
    private int id;
    @JsonProperty("NAME")
    private String name;
    @JsonProperty("ORDER")
    private int order;

    @Override
    @JsonIgnore
    public String getTableName() {
        return "categories";
    }

    @Override
    @JsonIgnore
    public String getOrderColumnName() {
        return "category_order";
    }
}
