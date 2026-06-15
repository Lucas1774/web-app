package com.lucas.server.components.shopping.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.common.mapper.UserMapper;
import com.lucas.server.components.shopping.dto.shopping.ShoppingItemDomain;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShoppingItemMapper implements EntityMapper<ShoppingItem, ShoppingItemDomain> {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    @Override
    public ShoppingItemDomain toDto(ShoppingItem entity) {
        if (null == entity) {
            return null;
        }
        return new ShoppingItemDomain(entity.getId(),
                null != entity.getUser() ? userMapper.toDto(entity.getUser()) : null,
                null != entity.getProduct() ? productMapper.toDto(entity.getProduct()) : null,
                entity.getQuantity());
    }

    @Override
    public ShoppingItem toEntity(ShoppingItemDomain dto) {
        if (null == dto) {
            return null;
        }
        ShoppingItem item = new ShoppingItem().setId(dto.getId()).setQuantity(dto.getQuantity());
        if (null != dto.getUser()) {
            item.setUser(userMapper.toEntity(dto.getUser()));
        }
        if (null != dto.getProduct()) {
            item.setProduct(productMapper.toEntity(dto.getProduct()));
        }
        return item;
    }
}
