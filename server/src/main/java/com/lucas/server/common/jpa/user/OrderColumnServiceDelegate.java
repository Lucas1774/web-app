package com.lucas.server.common.jpa.user;

import com.lucas.server.common.dto.DomainEntity;
import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.common.jpa.OrderColumnJpaService;
import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.shopping.dto.Sortable;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class OrderColumnServiceDelegate<T extends JpaEntity, D extends DomainEntity & Sortable, R extends JpaRepository<@NonNull T, Long>>
        extends GenericJpaServiceDelegate<T, D, R> implements OrderColumnJpaService<D> {

    protected OrderColumnServiceDelegate(R repository, EntityMapper<T, D> mapper) {
        super(repository, mapper);
    }

    @Override
    @Transactional
    public Set<D> updateOrders(OrderedIndexedSet<D> elements) {
        Set<D> toSave = new HashSet<>();
        for (int i = 0; i < elements.size(); i++) {
            D input = elements.get(i);
            D managed = repository.findById(input.getId()).map(mapper::toDto).orElseThrow();
            managed.setOrder(i + 1);
            toSave.add(managed);
        }
        return repository.saveAll(toSave.stream().map(mapper::toEntity).collect(Collectors.toSet()))
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toUnmodifiableSet());
    }
}
