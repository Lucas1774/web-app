package com.lucas.server.common.jpa.user;

import com.lucas.server.components.shopping.dto.Sortable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.List;

public class OrderColumnServiceDelegate<T extends Sortable> {

    private final JpaRepository<T, Long> repository;

    public OrderColumnServiceDelegate(JpaRepository<T, Long> repository) {
        this.repository = repository;
    }

    public List<T> updateOrders(List<T> elements) {
        List<T> toSave = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            T input = elements.get(i);
            T managed = this.repository.findById(input.getId()).orElseThrow();
            managed.setOrder(i + 1);
            toSave.add(managed);
        }
        return this.repository.saveAll(toSave);
    }
}
