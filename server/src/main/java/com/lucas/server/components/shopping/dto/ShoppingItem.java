package com.lucas.server.components.shopping.dto;

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
public class ShoppingItem implements Sortable {
    @JsonProperty("id")
    private int id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("categoryId")
    private Integer categoryId;
    @JsonProperty("category")
    private String category;
    @JsonProperty("categoryOrder")
    private Integer categoryOrder;
    @JsonProperty("quantity")
    private Integer quantity;
    @JsonProperty("isRare")
    private Boolean isRare;
    @JsonProperty("order")
    private int order;

    @Override
    @JsonIgnore
    public String getTableName() {
        return "products";
    }

    @Override
    @JsonIgnore
    public String getOrderColumnName() {
        return "product_order";
    }
}
