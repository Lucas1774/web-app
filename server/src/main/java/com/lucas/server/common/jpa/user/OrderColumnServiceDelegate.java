package com.lucas.server.common.jpa.user;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.common.jpa.OrderColumnJpaService;
import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.shopping.dto.Sortable;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

public abstract class OrderColumnServiceDelegate<T extends Sortable & JpaEntity, R extends JpaRepository<T, Long>>
        extends GenericJpaServiceDelegate<T, T, R> implements OrderColumnJpaService<T> {

    protected OrderColumnServiceDelegate(R repository, EntityMapper<T, T> mapper) {
        super(repository, mapper);
    }

    @Override
    @Transactional
    public Set<T> updateOrders(OrderedIndexedSet<T> elements) {
        Set<T> toSave = new HashSet<>();
        for (int i = 0; i < elements.size(); i++) {
            T input = elements.get(i);
            T managed = repository.findById(input.getId()).orElseThrow();
            managed.setOrder(i + 1);
            toSave.add(managed);
        }
        return new HashSet<>(repository.saveAll(toSave));
    }
}
