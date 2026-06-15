package com.lucas.server.components.shopping.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.lucas.server.components.shopping.dto.category.CategoryDomain;
import com.lucas.server.components.shopping.dto.product.ProductDomain;

/**
 * Interface for sortable entities in the shopping component, such as categories and products.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CategoryDomain.class, name = "categories"),
        @JsonSubTypes.Type(value = ProductDomain.class, name = "products")})
public interface Sortable {

    /**
     * @return the unique identifier of the sortable entity, used for order updates.
     */
    Long getId();

    /**
     * @return the order of the entity, which determines its position in a list. Lower values indicate higher priority.
     */
    Integer getOrder();

    /**
     * @param order the new order value to set for the entity. This should be a positive integer,
     *              where lower values indicate higher priority.
     * @return the sortable entity instance with the updated order value.
     */
    @SuppressWarnings("UnusedReturnValue")
    Sortable setOrder(Integer order);
}
