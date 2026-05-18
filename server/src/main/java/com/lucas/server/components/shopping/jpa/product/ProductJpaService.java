package com.lucas.server.components.shopping.jpa.product;

import com.lucas.server.common.jpa.user.OrderColumnServiceDelegate;
import com.lucas.server.common.jpa.user.UserRepository;
import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.shopping.jpa.category.Category;
import com.lucas.server.components.shopping.jpa.category.CategoryRepository;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItem;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProductJpaService extends OrderColumnServiceDelegate<Product, ProductRepository> {

    private final UserRepository userRepository;
    private final ShoppingItemRepository shoppingItemRepository;
    private final CategoryRepository categoryRepository;

    public ProductJpaService(ProductRepository repository,
                             EntityMapper<Product, Product> mapper,
                             UserRepository userRepository,
                             ShoppingItemRepository shoppingItemRepository,
                             CategoryRepository categoryRepository) {
        super(repository, mapper);
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
}
