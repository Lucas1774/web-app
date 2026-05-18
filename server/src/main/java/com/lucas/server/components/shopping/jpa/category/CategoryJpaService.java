package com.lucas.server.components.shopping.jpa.category;

import com.lucas.server.common.jpa.user.OrderColumnServiceDelegate;
import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryJpaService extends OrderColumnServiceDelegate<Category, CategoryRepository> {

    public CategoryJpaService(CategoryRepository repository, EntityMapper<Category, Category> mapper) {
        super(repository, mapper);
    }

    @Transactional(readOnly = true)
    public OrderedIndexedSet<Category> findAllByOrderByOrderAsc() {
        return repository.findAllByOrderByOrderAsc();
    }
}
