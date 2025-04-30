package com.lucas.server.components.shopping.jpa.shopping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {

    List<ShoppingItem> findAllByUser_Username(String username);

    Optional<ShoppingItem> findByUser_UsernameAndProduct_id(String username, Long productId);

    long countByProduct_Id(Long prodId);
}
