package com.lucas.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Category.class, name = "categories"),
        @JsonSubTypes.Type(value = ShoppingItem.class, name = "products")
})
public interface Sortable {

    @JsonIgnore
    String getTableName();

    @JsonIgnore
    String getOrderColumnName();

    @JsonIgnore
    int getId();

    @JsonIgnore
    int getOrder();
}
