package com.lucas.server.components.shopping.jpa.category;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.OrderColumnJpaService;
import com.lucas.server.common.jpa.user.OrderColumnServiceDelegate;
import com.lucas.server.common.mapper.IdentityEntityMapper;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class CategoryJpaService implements JpaService<Category>, OrderColumnJpaService<Category> {

    private final GenericJpaServiceDelegate<Category, Category, CategoryRepository> delegate;
    private final OrderColumnServiceDelegate<Category> orderColumnDelegate;
    private final CategoryRepository repository;

    public CategoryJpaService(CategoryRepository repository, IdentityEntityMapper<Category> identityEntityMapper) {
        delegate = new GenericJpaServiceDelegate<>(repository, identityEntityMapper);
        orderColumnDelegate = new OrderColumnServiceDelegate<>(repository);
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public OrderedIndexedSet<Category> findAllByOrderByOrderAsc() {
        return repository.findAllByOrderByOrderAsc();
    }

    @Override
    @Transactional
    public Set<Category> updateOrders(OrderedIndexedSet<Category> newlySortedElements) {
        return orderColumnDelegate.updateOrders(newlySortedElements);
    }

    @Override
    @Transactional
    public Set<Category> saveAll(Set<Category> elements) {
        return delegate.saveAll(elements);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Category> findAll() {
        return delegate.findAll();
    }

    @Override
    @Transactional
    public void deleteAll(Set<Category> elements) {
        delegate.deleteAll(elements);
    }
}
