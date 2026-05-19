package com.lucas.server.components.shopping.jpa.shopping;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.shopping.jpa.product.Product;
import com.lucas.server.components.shopping.jpa.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ShoppingItemJpaService
        extends GenericJpaServiceDelegate<ShoppingItem, ShoppingItem, ShoppingItemRepository> {

    private final ProductRepository productRepository;

    public ShoppingItemJpaService(ShoppingItemRepository repository,
                                  EntityMapper<ShoppingItem, ShoppingItem> mapper,
                                  ProductRepository productRepository) {
        super(repository, mapper);
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Set<ShoppingItem> findAllByUsername(String username) {
        return repository.findAllByUser_Username(username);
    }

    @Transactional
    public ShoppingItem updateShoppingItemQuantity(ShoppingItem input, String username) {
        ShoppingItem shoppingItem =
                repository.findByUser_UsernameAndProduct_Id(username, input.getProduct().getId()).orElseThrow();
        return shoppingItem.setQuantity(input.getQuantity());
    }

    @Transactional
    public Set<ShoppingItem> updateAllShoppingItemQuantities(String username, int quantity) {
        return repository.findAllByUser_Username(username)
                .stream()
                .map(item -> item.setQuantity(quantity))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public ShoppingItem deleteByProductAndUsernameRemoveOrphanedProductIfNecessary(Product input, String username) {
        ShoppingItem shoppingItem = repository.findByUser_UsernameAndProduct_Id(username, input.getId()).orElseThrow();
        repository.delete(shoppingItem);
        Long prodId = shoppingItem.getProduct().getId();
        long count = repository.countByProduct_Id(prodId);
        if (0 == count) {
            productRepository.deleteById(prodId);
        }
        return shoppingItem;
    }
}
