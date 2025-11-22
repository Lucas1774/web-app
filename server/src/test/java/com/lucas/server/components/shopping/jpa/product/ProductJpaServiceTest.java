package com.lucas.server.components.shopping.jpa.product;

import com.lucas.server.ConfiguredTest;
import com.lucas.server.common.jpa.user.UserJpaService;
import com.lucas.server.components.shopping.jpa.category.Category;
import com.lucas.server.components.shopping.jpa.category.CategoryJpaService;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItem;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItemJpaService;
import com.lucas.utils.OrderedIndexedSet;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ProductJpaServiceTest extends ConfiguredTest {

    @Autowired
    private ShoppingItemJpaService shoppingItemService;

    @Autowired
    private ProductJpaService productService;

    @Autowired
    private UserJpaService userService;

    @Autowired
    private CategoryJpaService categoryService;

    @Test
    @Transactional
    void testCreateProductAndOrLinkToUser() {
        // given: no products, admin user
        assertThat(productService.findAll()).isEmpty();
        userService.findByUsername("admin")
                .orElseThrow();

        // when & then
        // 1) first insertion of "X"
        Product p1 = productService.createProductAndOrLinkToUser("X", "admin").orElseThrow();
        // product should be created with order=1
        assertThat(p1.getOrder()).isEqualTo(1);
        // shopping item for admin & X should exist
        assertThat(shoppingItemService.findAllByUsername("admin"))
                .extracting(si -> si.getProduct().getName(), ShoppingItem::getQuantity)
                .containsExactly(tuple("X", 0));

        assertThat(productService.createProductAndOrLinkToUser("X", "admin")).isEmpty();

        // 2) third call with same args but other user → no new product, no new shopping row
        Product p1Again = productService.createProductAndOrLinkToUser("X", "default").orElseThrow();
        assertThat(p1Again.getId()).isEqualTo(p1.getId());
        assertThat(productService.findAll()).hasSize(1);
        assertThat(shoppingItemService.findAllByUsername("admin")).hasSize(1);

        // 3) insert a different product "Y" for same user
        Product p2 = productService.createProductAndOrLinkToUser("Y", "admin").orElseThrow();
        assertThat(p2.getOrder()).isEqualTo(2);

        // now two products exist
        assertThat(productService.findAll())
                .extracting(Product::getName, Product::getOrder)
                .containsExactlyInAnyOrder(
                        tuple("X", 1),
                        tuple("Y", 2)
                );

        // and admin has two shopping items, one per product
        assertThat(shoppingItemService.findAllByUsername("admin"))
                .hasSize(2)
                .extracting(si -> si.getProduct().getName(), ShoppingItem::getQuantity)
                .containsExactlyInAnyOrder(
                        tuple("X", 0),
                        tuple("Y", 0)
                );
    }


    @Test
    @Transactional
    void testUpdateProductCreateCategoryIfNecessary() {
        // given: an existing product for admin
        Product original = productService.createProductAndOrLinkToUser("Original", "admin").orElseThrow();

        // when: update name, mark as rare, change to new
        Category newCategory = new Category().setName("NewCat");
        Product updated = productService.updateProductCreateCategoryIfNecessary(
                new Product().setId(original.getId())
                        .setName("Updated").setIsRare(true)
                        .setCategory(newCategory));

        // then: values updated correctly
        assertThat(updated.getName()).isEqualTo("Updated");
        assertThat(updated.getIsRare()).isTrue();
        assertThat(updated.getCategory().getName()).isEqualTo("NewCat");
        // no new category created
        assertThat(categoryService.findAll())
                .extracting(Category::getName)
                .hasSize(1)
                .contains("NewCat");

        // when: update with a new category
        updated.setName("UpdatedAgain")
                .setIsRare(false)
                .setCategory(new Category().setId(updated.getCategory().getId()));
        Product updated2 = productService.updateProductCreateCategoryIfNecessary(updated);

        // then: new category created and assigned
        assertThat(updated2.getName()).isEqualTo("UpdatedAgain");
        assertThat(updated2.getIsRare()).isFalse();
        assertThat(updated2.getCategory().getName()).isEqualTo("NewCat");
        assertThat(categoryService.findAll())
                .hasSize(1)
                .extracting(Category::getName)
                .contains("NewCat");

        // when: update non-existent product id
        Product missing = new Product().setId(-999L);
        // then: throw exception
        assertThatThrownBy(() -> productService.updateProductCreateCategoryIfNecessary(missing))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @Transactional
    void testUpdateOrders() {
        // given: two categories with reversed order values
        Product p1 = new Product().setName("A").setOrder(2);
        Product p2 = new Product().setName("B").setOrder(1);
        Set<Product> saved = productService.createAll(Set.of(p1, p2));
        assertThat(saved)
                .extracting(Product::getName, Product::getOrder)
                .containsExactlyInAnyOrder(
                        tuple("A", 2),
                        tuple("B", 1)
                );

        // when: updating with "A" first "B" second
        OrderedIndexedSet<Product> input = saved.stream().sorted(Comparator.comparing(Product::getOrder).reversed())
                .collect(OrderedIndexedSet.toOrderedIndexedSet());
        Set<Product> result = productService.updateOrders(input);

        // then: orders should be reassigned to [1,2]
        assertThat(result)
                .extracting(Product::getName, Product::getOrder)
                .containsExactlyInAnyOrder(
                        tuple("A", 1),
                        tuple("B", 2)
                );
    }
}
