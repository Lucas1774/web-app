package com.lucas.server.common.jpa.user;

import com.lucas.server.components.shopping.dto.Sortable;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.HashSet;
import java.util.Set;

public class OrderColumnServiceDelegate<T extends Sortable> {

    private final JpaRepository<T, Long> repository;

    public OrderColumnServiceDelegate(JpaRepository<T, Long> repository) {
        this.repository = repository;
    }

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
