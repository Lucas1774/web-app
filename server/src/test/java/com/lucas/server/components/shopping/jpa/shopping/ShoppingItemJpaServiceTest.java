package com.lucas.server.components.shopping.jpa.shopping;

import com.lucas.server.ConfiguredTest;
import com.lucas.server.common.jpa.user.UserJpaService;
import com.lucas.server.components.shopping.dto.category.CategoryDomain;
import com.lucas.server.components.shopping.dto.product.ProductDomain;
import com.lucas.server.components.shopping.dto.shopping.ShoppingItemDomain;
import com.lucas.server.components.shopping.jpa.category.CategoryJpaService;
import com.lucas.server.components.shopping.jpa.product.ProductJpaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ShoppingItemJpaServiceTest extends ConfiguredTest {

    @Autowired
    private ShoppingItemJpaService shoppingItemService;

    @Autowired
    private ProductJpaService productService;

    @Autowired
    private CategoryJpaService categoryService;

    @Autowired
    private UserJpaService userService;

    @Test
    void findAllByUsername() {
        CategoryDomain cat1 = new CategoryDomain().setName("C1").setOrder(1);
        CategoryDomain cat2 = new CategoryDomain().setName("C2").setOrder(2);
        Set<CategoryDomain> saved = categoryService.saveAll(Set.of(cat1, cat2));
        CategoryDomain c1 = saved.stream().filter(c -> "C1".equals(c.getName())).findFirst().orElseThrow();
        CategoryDomain c2 = saved.stream().filter(c -> "C2".equals(c.getName())).findFirst().orElseThrow();

        ProductDomain prodA = new ProductDomain().setName("ProdA").setIsRare(false).setCategory(c1).setOrder(100);
        ProductDomain prodB = new ProductDomain().setName("ProdB").setIsRare(false).setCategory(c2).setOrder(200);
        Set<ProductDomain> products = productService.saveAll(Set.of(prodA, prodB));
        ProductDomain pa = products.stream().filter(p -> "ProdA".equals(p.getName())).findFirst().orElseThrow();
        ProductDomain pb = products.stream().filter(p -> "ProdB".equals(p.getName())).findFirst().orElseThrow();

        shoppingItemService.saveAll(Set.of(new ShoppingItemDomain().setUser(userService.findByUsername("admin")
                        .orElseThrow()).setProduct(pb).setQuantity(5),
                new ShoppingItemDomain().setUser(userService.findByUsername("default").orElseThrow())
                        .setProduct(pa)
                        .setQuantity(3)));

        // when & then: only ProdB is returned
        assertThat(shoppingItemService.findAllByUsername("admin")).hasSize(1)
                .extracting(si -> si.getProduct().getName(),
                        ShoppingItemDomain::getQuantity,
                        si -> si.getProduct().getOrder())
                .containsExactly(tuple("ProdB", 5, 200));
    }

    @Test
    void updateAllShoppingItemQuantities() {
        // given: multiple items for admin
        ProductDomain p1 = productService.createProductAndOrLinkToUser("P1", "admin").orElseThrow();
        ProductDomain p2 = productService.createProductAndOrLinkToUser("P2", "admin").orElseThrow();

        // initial quantities differ
        shoppingItemService.updateShoppingItemQuantity(new ShoppingItemDomain().setProduct(p1).setQuantity(1), "admin");
        shoppingItemService.updateShoppingItemQuantity(new ShoppingItemDomain().setProduct(p2).setQuantity(2), "admin");

        // when: set all to 7
        Set<ShoppingItemDomain> updated = shoppingItemService.updateAllShoppingItemQuantities("admin", 7);

        // then: both items have quantity 7
        assertThat(updated).hasSize(2).extracting(ShoppingItemDomain::getQuantity).containsExactlyInAnyOrder(7, 7);
        assertThat(shoppingItemService.findAllByUsername("admin")).extracting(ShoppingItemDomain::getQuantity)
                .containsExactlyInAnyOrder(7, 7);
    }

    @Test
    void updateShoppingItemQuantity() {
        // given: admin item auto-created and an item for default user
        ProductDomain prod = productService.createProductAndOrLinkToUser("P", "admin").orElseThrow();
        shoppingItemService.saveAll(Set.of(new ShoppingItemDomain().setUser(userService.findByUsername("default")
                .orElseThrow()).setProduct(prod).setQuantity(2)));

        // when: update admin quantity to 10
        ShoppingItemDomain updated =
                shoppingItemService.updateShoppingItemQuantity(new ShoppingItemDomain().setProduct(prod)
                        .setQuantity(10), "admin");

        // then: only admin item updated
        assertThat(updated.getQuantity()).isEqualTo(10);
        assertThat(shoppingItemService.findAllByUsername("admin")).extracting(ShoppingItemDomain::getQuantity)
                .containsExactly(10);
        assertThat(shoppingItemService.findAllByUsername("default")).extracting(ShoppingItemDomain::getQuantity)
                .containsExactly(2);
    }

    @Test
    void deleteByProductAndUsernameRemoveOrphanedProductIfNecessary() {
        // given: one item per user
        ProductDomain p = productService.createProductAndOrLinkToUser("ToDel", "admin").orElseThrow();
        shoppingItemService.saveAll(Set.of(new ShoppingItemDomain().setUser(userService.findByUsername("default")
                .orElseThrow()).setProduct(p).setQuantity(5)));
        // admin already has item from insertProduct
        assertThat(shoppingItemService.findAll()).hasSize(2);
        assertThat(productService.findAll()).hasSize(1);

        // when: delete admin's item
        ShoppingItemDomain deleted =
                shoppingItemService.deleteByProductAndUsernameRemoveOrphanedProductIfNecessary(p, "admin");

        // then: product still exists, only default remains
        assertThat(deleted.getUser().getUsername()).isEqualTo("admin");
        assertThat(productService.findAll()).isNotEmpty();
        assertThat(shoppingItemService.findAll()).hasSize(1)
                .extracting(si -> si.getUser().getUsername())
                .containsExactly("default");

        // when: delete default's item
        shoppingItemService.deleteByProductAndUsernameRemoveOrphanedProductIfNecessary(p, "default");

        // then: no products or shopping items remain
        assertThat(shoppingItemService.findAll()).isEmpty();
        assertThat(productService.findAll()).isEmpty();
    }
}
