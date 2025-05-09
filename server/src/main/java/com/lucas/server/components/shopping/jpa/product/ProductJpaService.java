package com.lucas.server.components.shopping.jpa.product;


import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.OrderColumnJpaService;
import com.lucas.server.common.jpa.user.OrderColumnServiceDelegate;
import com.lucas.server.common.jpa.user.UserRepository;
import com.lucas.server.components.shopping.jpa.category.Category;
import com.lucas.server.components.shopping.jpa.category.CategoryRepository;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItem;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItemRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductJpaService implements JpaService<Product>, OrderColumnJpaService<Product> {

    private final ProductRepository repository;
    private final UserRepository userRepository;
    private final ShoppingItemRepository shoppingItemRepository;
    private final CategoryRepository categoryRepository;
    private final OrderColumnServiceDelegate<Product> orderColumnServiceDelegate;

    public ProductJpaService(ProductRepository repository, UserRepository userRepository, ShoppingItemRepository shoppingItemRepository,
                             CategoryRepository categoryRepository, OrderColumnServiceDelegate<Product> orderColumnServiceDelegate) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.shoppingItemRepository = shoppingItemRepository;
        this.categoryRepository = categoryRepository;
        this.orderColumnServiceDelegate = orderColumnServiceDelegate;
    }

    @Override
    public Optional<Product> save(Product entity) {
        return Optional.of(this.repository.save(entity));
    }

    @Override
    public List<Product> saveAll(Iterable<Product> entities) {
        return this.repository.saveAll(entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<Product> findAll() {
        return this.repository.findAll();
    }

    @Override
    public Optional<Product> findById(Long id) {
        return this.repository.findById(id);
    }

    @Transactional
    public Optional<Product> createProductAndOrLinkToUser(String productName, String username) {
        Product product = this.repository.findByName(productName)
                .orElseGet(() -> {
                    int newOrder = this.repository.findTopByOrderByOrderDesc()
                            .map(p -> p.getOrder() + 1)
                            .orElse(1);
                    return this.repository.save(new Product()
                            .setName(productName)
                            .setOrder(newOrder));
                });
        if (this.shoppingItemRepository.findByUser_UsernameAndProduct_id(username, product.getId()).isPresent()) {
            return Optional.empty();
        }
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

        product.setCategory(category);
        return repository.save(product);
    }

    @Override
    public List<Product> updateOrders(List<Product> newlySortedElements) {
        return this.orderColumnServiceDelegate.sort(this.repository, newlySortedElements);
    }
}
