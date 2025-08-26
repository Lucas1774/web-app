package com.lucas.server.components.shopping.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.lucas.server.components.shopping.jpa.category.Category;
import com.lucas.server.components.shopping.jpa.product.Product;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Category.class, name = "categories"),
        @JsonSubTypes.Type(value = Product.class, name = "products")
})
public interface Sortable {

    Long getId();

    @SuppressWarnings("unused")
    Integer getOrder();

    @SuppressWarnings("UnusedReturnValue")
    Sortable setOrder(Integer order);
}
