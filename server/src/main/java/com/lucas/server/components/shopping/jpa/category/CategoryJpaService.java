package com.lucas.server.components.shopping.jpa.category;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.OrderColumnJpaService;
import com.lucas.server.common.jpa.user.OrderColumnServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryJpaService implements JpaService<Category>, OrderColumnJpaService<Category> {

    @Delegate
    private final GenericJpaServiceDelegate<Category, CategoryRepository> delegate;
    @Delegate
    private final OrderColumnServiceDelegate<Category> orderColumnDelegate;
    private final CategoryRepository repository;

    public CategoryJpaService(CategoryRepository repository) {
        delegate = new GenericJpaServiceDelegate<>(repository);
        orderColumnDelegate = new OrderColumnServiceDelegate<>(repository);
        this.repository = repository;
    }

    public List<Category> findAllByOrderByOrderAsc() {
        return repository.findAllByOrderByOrderAsc();
    }
}
