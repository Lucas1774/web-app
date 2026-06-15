package com.lucas.server.components.shopping.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.shopping.dto.category.CategoryDomain;
import com.lucas.server.components.shopping.jpa.category.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper implements EntityMapper<Category, CategoryDomain> {

    @Override
    public CategoryDomain toDto(Category entity) {
        if (null == entity) {
            return null;
        }
        return new CategoryDomain(entity.getId(), entity.getName(), entity.getOrder());
    }

    @Override
    public Category toEntity(CategoryDomain dto) {
        if (null == dto) {
            return null;
        }
        return new Category().setId(dto.getId()).setName(dto.getName()).setOrder(dto.getOrder());
    }
}
