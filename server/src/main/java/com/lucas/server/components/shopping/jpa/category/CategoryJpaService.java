package com.lucas.server.components.shopping.jpa.category;

import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.OrderColumnJpaService;
import com.lucas.server.common.jpa.user.OrderColumnServiceDelegate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryJpaService implements JpaService<Category>, OrderColumnJpaService<Category> {

    private final CategoryRepository repository;
    private final OrderColumnServiceDelegate<Category> orderColumnServiceDelegate;

    public CategoryJpaService(CategoryRepository repository, OrderColumnServiceDelegate<Category> orderColumnServiceDelegate) {
        this.repository = repository;
        this.orderColumnServiceDelegate = orderColumnServiceDelegate;
    }

    @Override
    public Optional<Category> save(Category entity) {
        return Optional.of(this.repository.save(entity));
    }

    @Override
    public List<Category> saveAll(Iterable<Category> entities) {
        return this.repository.saveAll(entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<Category> findAll() {
        return this.repository.findAll();
    }

    @Override
    public Optional<Category> findById(Long id) {
        return this.repository.findById(id);
    }

    public List<Category> findAllByOrderByOrderAsc() {
        return this.repository.findAllByOrderByOrderAsc();
    }

    @Override
    public List<Category> updateOrders(List<Category> newlySortedElements) {
        return this.orderColumnServiceDelegate.sort(this.repository, newlySortedElements);
    }
}
