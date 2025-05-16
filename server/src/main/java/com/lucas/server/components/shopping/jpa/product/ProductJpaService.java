package com.lucas.server.components.shopping.jpa.product;


import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.OrderColumnJpaService;
import com.lucas.server.common.jpa.user.OrderColumnServiceDelegate;
import com.lucas.server.common.jpa.user.UserRepository;
import com.lucas.server.components.shopping.jpa.category.Category;
import com.lucas.server.components.shopping.jpa.category.CategoryRepository;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItem;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItemRepository;
import jakarta.transaction.Transactional;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProductJpaService implements JpaService<Product>, OrderColumnJpaService<Product> {

    @Delegate
    private final GenericJpaServiceDelegate<Product, ProductRepository> delegate;
    @Delegate
    private final OrderColumnServiceDelegate<Product> orderColumnDelegate;
    private final ProductRepository repository;
    private final UserRepository userRepository;
    private final ShoppingItemRepository shoppingItemRepository;
    private final CategoryRepository categoryRepository;

    public ProductJpaService(ProductRepository repository, UserRepository userRepository,
                             ShoppingItemRepository shoppingItemRepository, CategoryRepository categoryRepository) {
        this.delegate = new GenericJpaServiceDelegate<>(repository);
        this.orderColumnDelegate = new OrderColumnServiceDelegate<>(repository);
        this.repository = repository;
        this.userRepository = userRepository;
        this.shoppingItemRepository = shoppingItemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Optional<Product> createProductAndOrLinkToUser(String productName, String username) {
        if (this.shoppingItemRepository.findByUser_UsernameAndProduct_Name(username, productName).isPresent()) {
            return Optional.empty();
        }

        Product product = this.repository.findByName(productName)
                .orElseGet(() -> {
                    int newOrder = this.repository.findTopByOrderByOrderDesc()
                            .map(p -> p.getOrder() + 1)
                            .orElse(1);
                    return this.repository.save(new Product()
                            .setName(productName)
                            .setOrder(newOrder));
                });
        this.shoppingItemRepository.save(new ShoppingItem()
                .setUser(this.userRepository.findByUsername(username).orElse(null))
                .setProduct(product)
                .setQuantity(0));

        return Optional.of(product);
    }

    @Transactional
    public Product updateProductCreateCategoryIfNecessary(Product input) {
        Product product = repository.findById(input.getId()).orElseThrow();
        product.setName(input.getName()).setIsRare(input.getIsRare());

        Category category;
        if (null != input.getCategory().getId()) {
            category = categoryRepository.findById(input.getCategory().getId()).orElseThrow();
        } else {
            Integer maxOrder = categoryRepository.findFirstByOrderByOrderDesc()
                    .map(c -> c.getOrder() + 1)
                    .orElse(1);
            category = categoryRepository.save(new Category()
                    .setName(input.getCategory().getName())
                    .setOrder(maxOrder));
        }

        return product.setCategory(category);
    }
}
