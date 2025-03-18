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
public class ShoppingItem implements Sortable {
    @JsonProperty("ID")
    private int id;
    @JsonProperty("NAME")
    private String name;
    @JsonProperty("CATEGORY_ID")
    private Integer categoryId;
    @JsonProperty("CATEGORY")
    private String category;
    @JsonProperty("CATEGORY_ORDER")
    private Integer categoryOrder;
    @JsonProperty("QUANTITY")
    private Integer quantity;
    @JsonProperty("IS_RARE")
    private Boolean isRare;
    @JsonProperty("ORDER")
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
