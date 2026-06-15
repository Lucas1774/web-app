package com.lucas.server.components.shopping.jpa.category;

import com.lucas.server.common.jpa.user.OrderColumnServiceDelegate;
import com.lucas.server.components.shopping.dto.category.CategoryDomain;
import com.lucas.server.components.shopping.mapper.CategoryMapper;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryJpaService extends OrderColumnServiceDelegate<Category, CategoryDomain, CategoryRepository> {

    public CategoryJpaService(CategoryRepository repository, CategoryMapper mapper) {
        super(repository, mapper);
    }

    @Transactional(readOnly = true)
    public OrderedIndexedSet<CategoryDomain> findAllByOrderByOrderAsc() {
        return repository.findAllByOrderByOrderAsc()
                .stream()
                .map(mapper::toDto)
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }
}
