package com.lucas.server.components.shopping.jpa.product;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.OrderColumnJpaService;
import com.lucas.server.common.jpa.user.OrderColumnServiceDelegate;
import com.lucas.server.common.jpa.user.UserRepository;
import com.lucas.server.common.mapper.IdentityEntityMapper;
import com.lucas.server.components.shopping.jpa.category.Category;
import com.lucas.server.components.shopping.jpa.category.CategoryRepository;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItem;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItemRepository;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class ProductJpaService implements JpaService<Product>, OrderColumnJpaService<Product> {

    private final GenericJpaServiceDelegate<Product, Product, ProductRepository> delegate;
    private final OrderColumnServiceDelegate<Product> orderColumnDelegate;
    private final ProductRepository repository;
    private final UserRepository userRepository;
    private final ShoppingItemRepository shoppingItemRepository;
    private final CategoryRepository categoryRepository;

    public ProductJpaService(ProductRepository repository, UserRepository userRepository,
                             ShoppingItemRepository shoppingItemRepository, CategoryRepository categoryRepository, IdentityEntityMapper<Product> identityEntityMapper) {
        delegate = new GenericJpaServiceDelegate<>(repository, identityEntityMapper);
        orderColumnDelegate = new OrderColumnServiceDelegate<>(repository);
        this.repository = repository;
        this.userRepository = userRepository;
        this.shoppingItemRepository = shoppingItemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Optional<Product> createProductAndOrLinkToUser(String productName, String username) {
        if (shoppingItemRepository.findByUser_UsernameAndProduct_Name(username, productName).isPresent()) {
            return Optional.empty();
        }

        Product product = repository.findByName(productName)
                .orElseGet(() -> {
                    int newOrder = repository.findTopByOrderByOrderDesc()
                            .map(p -> p.getOrder() + 1)
                            .orElse(1);
                    return repository.save(new Product()
                            .setName(productName)
                            .setOrder(newOrder));
                });
        shoppingItemRepository.save(new ShoppingItem()
                .setUser(userRepository.findByUsername(username).orElse(null))
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
            Integer maxOrder = categoryRepository.findTopByOrderByOrderDesc()
                    .map(c -> c.getOrder() + 1)
                    .orElse(1);
            category = categoryRepository.save(new Category()
                    .setName(input.getCategory().getName())
                    .setOrder(maxOrder));
        }

        return product.setCategory(category);
    }

    @Override
    @Transactional
    public Set<Product> updateOrders(OrderedIndexedSet<Product> newlySortedElements) {
        return orderColumnDelegate.updateOrders(newlySortedElements);
    }

    @Override
    @Transactional
    public Set<Product> saveAll(Set<Product> elements) {
        return delegate.saveAll(elements);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Product> findAll() {
        return delegate.findAll();
    }

    @Override
    @Transactional
    public void deleteAll(Set<Product> elements) {
        delegate.deleteAll(elements);
    }
}
