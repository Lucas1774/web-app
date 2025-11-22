package com.lucas.server.components.shopping.jpa.category;

import com.lucas.utils.OrderedIndexedSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    OrderedIndexedSet<Category> findAllByOrderByOrderAsc();

    Optional<Category> findTopByOrderByOrderDesc();
}
