package com.lucas.server.components.shopping.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.shopping.dto.product.ProductDomain;
import com.lucas.server.components.shopping.jpa.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static java.lang.Boolean.TRUE;

@Component
@RequiredArgsConstructor
public class ProductMapper implements EntityMapper<Product, ProductDomain> {

    private final CategoryMapper categoryMapper;

    @Override
    public ProductDomain toDto(Product entity) {
        if (null == entity) {
            return null;
        }
        return new ProductDomain(entity.getId(),
                entity.getName(),
                entity.isRare(),
                null != entity.getCategory() ? categoryMapper.toDto(entity.getCategory()) : null,
                entity.getOrder());
    }

    @Override
    public Product toEntity(ProductDomain dto) {
        if (null == dto) {
            return null;
        }
        Product product = new Product().setId(dto.getId())
                .setName(dto.getName())
                .setRare(TRUE.equals(dto.getIsRare()))
                .setOrder(dto.getOrder());

        if (null != dto.getCategory()) {
            product.setCategory(categoryMapper.toEntity(dto.getCategory()));
        }
        return product;
    }
}
