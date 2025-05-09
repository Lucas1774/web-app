package com.lucas.server.components.shopping.jpa.shopping;

import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.components.shopping.jpa.product.Product;
import com.lucas.server.components.shopping.jpa.product.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShoppingItemJpaService implements JpaService<ShoppingItem> {

    private final ShoppingItemRepository repository;
    private final UniqueConstraintWearyJpaServiceDelegate<ShoppingItem> delegate;
    private final ProductRepository productRepository;

    public ShoppingItemJpaService(ShoppingItemRepository repository, UniqueConstraintWearyJpaServiceDelegate<ShoppingItem> delegate, ProductRepository productRepository) {
        this.repository = repository;
        this.delegate = delegate;
        this.productRepository = productRepository;
    }

    @Override
    public Optional<ShoppingItem> save(ShoppingItem entity) {
        return this.delegate.save(this.repository, entity);
    }

    @Override
    public List<ShoppingItem> saveAll(Iterable<ShoppingItem> entities) {
        return this.delegate.saveAllIgnoringDuplicates(this.repository, entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<ShoppingItem> findAll() {
        return this.repository.findAll();
    }

    @Override
    public Optional<ShoppingItem> findById(Long id) {
        return this.repository.findById(id);
    }

    public List<ShoppingItem> findAllByUsername(String username) {
        return this.repository.findAllByUser_Username(username);
    }

    public ShoppingItem updateShoppingItemQuantity(ShoppingItem input, String username) {
        ShoppingItem shoppingItem = this.repository.findByUser_UsernameAndProduct_id(username, input.getProduct().getId())
                .orElseThrow();
        shoppingItem.setQuantity(input.getQuantity());
        return this.repository.save(shoppingItem);
    }

    public List<ShoppingItem> updateAllShoppingItemQuantities(String username, int quantity) {
        List<ShoppingItem> shoppingItems = this.repository.findAllByUser_Username(username);
        shoppingItems.forEach(shoppingItem -> shoppingItem.setQuantity(quantity));
        return this.repository.saveAll(shoppingItems);
    }

    @Transactional
    public ShoppingItem deleteByProductAndUsernameRemoveOrphanedProductIfNecessary(Product input, String username) {
        ShoppingItem shoppingItem = this.repository.findByUser_UsernameAndProduct_id(username, input.getId())
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
