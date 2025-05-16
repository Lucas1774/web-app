package com.lucas.server.components.shopping.jpa.shopping;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.components.shopping.jpa.product.Product;
import com.lucas.server.components.shopping.jpa.product.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShoppingItemJpaService implements JpaService<ShoppingItem> {

    @Delegate
    private final GenericJpaServiceDelegate<ShoppingItem, ShoppingItemRepository> delegate;
    private final ShoppingItemRepository repository;
    private final ProductRepository productRepository;

    public ShoppingItemJpaService(ShoppingItemRepository repository, ProductRepository productRepository) {
        this.delegate = new GenericJpaServiceDelegate<>(repository);
        this.repository = repository;
        this.productRepository = productRepository;
    }

    public List<ShoppingItem> findAllByUsername(String username) {
        return this.repository.findAllByUser_Username(username);
    }

    @Transactional
    public ShoppingItem updateShoppingItemQuantity(ShoppingItem input, String username) {
        ShoppingItem shoppingItem = this.repository.findByUser_UsernameAndProduct_Id(username, input.getProduct().getId())
                .orElseThrow();
        return shoppingItem.setQuantity(input.getQuantity());
    }

    @Transactional
    public List<ShoppingItem> updateAllShoppingItemQuantities(String username, int quantity) {
        return this.repository.findAllByUser_Username(username).stream()
                .map(item -> item.setQuantity(quantity)).toList();
    }

    @Transactional
    public ShoppingItem deleteByProductAndUsernameRemoveOrphanedProductIfNecessary(Product input, String username) {
        ShoppingItem shoppingItem = this.repository.findByUser_UsernameAndProduct_Id(username, input.getId())
                .orElseThrow();
        this.repository.delete(shoppingItem);
        Long prodId = shoppingItem.getProduct().getId();
        long count = this.repository.countByProduct_Id(prodId);
        if (count == 0) {
            this.productRepository.deleteById(prodId);
        }
        return shoppingItem;
    }
}
