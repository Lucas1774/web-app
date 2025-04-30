package com.lucas.server.common.jpa.user;

import com.lucas.server.components.shopping.dto.Sortable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderColumnServiceDelegate<T extends Sortable> {

    public List<T> sort(JpaRepository<T, Long> repository, List<T> elements) {
        List<T> toSave = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            T input = elements.get(i);
            T managed = repository.findById(input.getId()).orElseThrow();
            managed.setOrder(i + 1);
            toSave.add(managed);
        }
        return repository.saveAll(toSave);
    }
}
