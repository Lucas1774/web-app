package com.lucas.server.components.shopping.jpa.shopping;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.components.shopping.dto.product.ProductDomain;
import com.lucas.server.components.shopping.dto.shopping.ShoppingItemDomain;
import com.lucas.server.components.shopping.jpa.product.ProductRepository;
import com.lucas.server.components.shopping.mapper.ShoppingItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ShoppingItemJpaService
        extends GenericJpaServiceDelegate<ShoppingItem, ShoppingItemDomain, ShoppingItemRepository> {

    private final ProductRepository productRepository;

    public ShoppingItemJpaService(ShoppingItemRepository repository,
                                  ShoppingItemMapper mapper,
                                  ProductRepository productRepository) {
        super(repository, mapper);
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Set<ShoppingItemDomain> findAllByUsername(String username) {
        return repository.findAllByUser_Username(username)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public ShoppingItemDomain updateShoppingItemQuantity(ShoppingItemDomain input, String username) {
        ShoppingItem shoppingItem =
                repository.findByUser_UsernameAndProduct_Id(username, input.getProduct().getId()).orElseThrow();
        return mapper.toDto(shoppingItem.setQuantity(input.getQuantity()));
    }

    @Transactional
    public Set<ShoppingItemDomain> updateAllShoppingItemQuantities(String username, int quantity) {
        return repository.findAllByUser_Username(username)
                .stream()
                .map(item -> item.setQuantity(quantity))
                .map(mapper::toDto)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public ShoppingItemDomain deleteByProductAndUsernameRemoveOrphanedProductIfNecessary(ProductDomain input,
                                                                                         String username) {
        ShoppingItem shoppingItem = repository.findByUser_UsernameAndProduct_Id(username, input.getId()).orElseThrow();
        repository.delete(shoppingItem);
        Long prodId = shoppingItem.getProduct().getId();
        long count = repository.countByProduct_Id(prodId);
        if (0 == count) {
            productRepository.deleteById(prodId);
        }
        return mapper.toDto(shoppingItem);
    }
}
