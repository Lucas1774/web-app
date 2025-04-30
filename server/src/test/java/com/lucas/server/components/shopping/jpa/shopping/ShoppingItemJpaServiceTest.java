package com.lucas.server.components.shopping.jpa.shopping;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.jpa.user.UserJpaService;
import com.lucas.server.components.shopping.jpa.category.Category;
import com.lucas.server.components.shopping.jpa.category.CategoryJpaService;
import com.lucas.server.components.shopping.jpa.product.Product;
import com.lucas.server.components.shopping.jpa.product.ProductJpaService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ShoppingItemJpaServiceTest {

    @Autowired
    ShoppingItemJpaService shoppingItemService;

    @Autowired
    ProductJpaService productService;

    @Autowired
    CategoryJpaService categoryService;

    @Autowired
    UserJpaService userService;

    @AfterEach
    void tearDown() {
        shoppingItemService.deleteAll();
        productService.deleteAll();
        categoryService.deleteAll();
    }

    @Test
    void testFindAllByUsername() {
        Category c1 = new Category().setName("C1").setOrder(1);
        Category c2 = new Category().setName("C2").setOrder(2);
        categoryService.saveAll(List.of(c1, c2));

        Product prodA = new Product()
                .setName("ProdA")
                .setIsRare(false)
                .setCategory(c1)
                .setOrder(100);
        Product prodB = new Product()
                .setName("ProdB")
                .setIsRare(false)
                .setCategory(c2)
                .setOrder(200);
        productService.saveAll(List.of(prodA, prodB));

        shoppingItemService.saveAll(List.of(
                new ShoppingItem()
                        .setUser(userService.findByUsername("admin").orElseThrow())
                        .setProduct(prodB)
                        .setQuantity(5),
                new ShoppingItem()
                        .setUser(userService.findByUsername("default").orElseThrow())
                        .setProduct(prodA)
                        .setQuantity(3)
        ));

        // when & then: only ProdB is returned
        assertThat(shoppingItemService.findAllByUsername("admin"))
                .hasSize(1)
                .extracting(
                        s -> s.getProduct().getName(),
                        ShoppingItem::getQuantity,
                        s -> s.getProduct().getOrder()
                )
                .containsExactly(
                        tuple("ProdB", 5, 200)
                );
    }

    @Test
    @Transactional
    void testUpdateAllShoppingItemQuantities() {
        // given: multiple items for admin
        Product p1 = productService.createProductAndOrLinkToUser("P1", "admin").orElseThrow();
        Product p2 = productService.createProductAndOrLinkToUser("P2", "admin").orElseThrow();

        // initial quantities differ
        shoppingItemService.updateShoppingItemQuantity(new ShoppingItem().setProduct(p1).setQuantity(1), "admin");
        shoppingItemService.updateShoppingItemQuantity(new ShoppingItem().setProduct(p2).setQuantity(2), "admin");

        // when: set all to 7
        List<ShoppingItem> updated = shoppingItemService.updateAllShoppingItemQuantities("admin", 7);

        // then: both items have quantity 7
        assertThat(updated).hasSize(2)
                .extracting(ShoppingItem::getQuantity)
                .containsExactlyInAnyOrder(7, 7);
        assertThat(shoppingItemService.findAllByUsername("admin"))
                .extracting(ShoppingItem::getQuantity)
                .containsExactlyInAnyOrder(7, 7);
    }

    @Test
    @Transactional
    void testUpdateShoppingItemQuantity() {
        // given: admin item auto-created and an item for default user
        Product prod = productService.createProductAndOrLinkToUser("P", "admin").orElseThrow();
        shoppingItemService.save(
                new ShoppingItem()
                        .setUser(userService.findByUsername("default").orElseThrow())
                        .setProduct(prod).setQuantity(2)
        );

        // when: update admin quantity to 10
        ShoppingItem updated = shoppingItemService.updateShoppingItemQuantity(
                new ShoppingItem().setProduct(prod).setQuantity(10), "admin");

        // then: only admin item updated
        assertThat(updated.getQuantity()).isEqualTo(10);
        assertThat(shoppingItemService.findAllByUsername("admin")).extracting(ShoppingItem::getQuantity)
                .containsExactly(10);
        assertThat(shoppingItemService.findAllByUsername("default")).extracting(ShoppingItem::getQuantity)
                .containsExactly(2);
    }

    @Test
    @Transactional
    void deleteByProductAndUsernameRemoveOrphanedProductIfNecessary() {
        // given: one item per user
        Product p = productService.createProductAndOrLinkToUser("ToDel", "admin").orElseThrow();
        shoppingItemService.save(
                new ShoppingItem().setUser(userService.findByUsername("default").orElseThrow())
                        .setProduct(p).setQuantity(5)
        );
        // admin already has item from insertProduct
        assertThat(shoppingItemService.findAll()).hasSize(2);
        assertThat(productService.findAll()).hasSize(1);

        // when: delete admin's item
        ShoppingItem deleted = shoppingItemService.deleteByProductAndUsernameRemoveOrphanedProductIfNecessary(p, "admin");

        // then: product still exists, only default remains
        assertThat(deleted.getUser().getUsername()).isEqualTo("admin");
        assertThat(productService.findAll()).isNotEmpty();
        assertThat(shoppingItemService.findAll())
                .hasSize(1)
                .extracting(si -> si.getUser().getUsername())
                .containsExactly("default");

        // when: delete default's item
        shoppingItemService.deleteByProductAndUsernameRemoveOrphanedProductIfNecessary(p, "default");

        // then: no products or shopping items remain
        assertThat(shoppingItemService.findAll()).isEmpty();
        assertThat(productService.findAll()).isEmpty();
    }
}
