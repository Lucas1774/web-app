package com.lucas.server.connection;

import com.lucas.server.ConfiguredTest;
import com.lucas.server.components.shopping.dto.category.CategoryDomain;
import com.lucas.server.components.shopping.dto.product.ProductDomain;
import com.lucas.server.components.shopping.dto.shopping.ShoppingItemDomain;
import com.lucas.server.components.shopping.jpa.category.CategoryJpaService;
import com.lucas.server.components.shopping.jpa.product.ProductJpaService;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItemJpaService;
import com.lucas.server.components.sudoku.dto.SudokuDomain;
import com.lucas.server.components.sudoku.jpa.SudokuJpaService;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

class DataAccessTest extends ConfiguredTest {

    @Autowired
    private ShoppingItemJpaService shoppingItemService;

    @Autowired
    private CategoryJpaService categoryService;

    @Autowired
    private ProductJpaService productService;

    @Autowired
    private SudokuJpaService sudokuService;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shoppingCrud() {
        // seed user and category
        jdbcTemplate.getJdbcOperations().execute("INSERT INTO users(username, password) VALUES('bob','pwd')");
        jdbcTemplate.getJdbcOperations().execute("INSERT INTO categories(name, category_order) VALUES('catA',1)");

        // insert product and assign
        productService.createProductAndOrLinkToUser("item1", "bob");
        Set<ShoppingItemDomain> items = shoppingItemService.findAllByUsername("bob");
        assertThat(items).hasSize(1);
        ShoppingItemDomain item = items.stream().findFirst().orElseThrow();
        assertThat(item.getProduct().getName()).isEqualTo("item1");

        // update quantity
        assertThat(shoppingItemService.updateShoppingItemQuantity(new ShoppingItemDomain().setProduct(item.getProduct())
                .setQuantity(5), "bob").getQuantity()).isEqualTo(5);

        // remove
        shoppingItemService.deleteByProductAndUsernameRemoveOrphanedProductIfNecessary(item.getProduct(), "bob");
        items = shoppingItemService.findAllByUsername("bob");
        assertThat(items).isEmpty();
    }

    @Test
    void insertAndRetrieveSudoku() {
        SudokuDomain s1 = SudokuDomain.withDefaultValues();
        sudokuService.createIgnoringDuplicates(Collections.singleton(s1));
        assertThat(sudokuService.findAll()).hasSize(1)
                .extracting(SudokuDomain::getState)
                .containsExactly(s1.getState());
    }

    @Test
    void getPossibleCategories() {
        jdbcTemplate.getJdbcOperations()
                .execute("INSERT INTO categories(name, category_order) VALUES('x',10),( 'y',20 )");
        Set<CategoryDomain> cats = categoryService.findAllByOrderByOrderAsc();
        assertThat(cats).extracting(CategoryDomain::getName).containsExactly("x", "y");
    }

    @Test
    void updateOrders() {
        // seed categories
        jdbcTemplate.getJdbcOperations().execute("INSERT INTO categories(name, category_order) VALUES('a',1),('b',2)");
        OrderedIndexedSet<CategoryDomain> cats = categoryService.findAllByOrderByOrderAsc();
        // swap order
        CategoryDomain first = cats.get(0);
        CategoryDomain second = cats.get(1);

        categoryService.updateOrders(OrderedIndexedSet.of(second, first));
        Set<CategoryDomain> updated = categoryService.findAllByOrderByOrderAsc();
        assertThat(updated).extracting(CategoryDomain::getName, CategoryDomain::getOrder)
                .containsExactlyInAnyOrder(tuple("b", 1), tuple("a", 2));
    }

    @Test
    void updateProductWithExistingCategory() {
        // seed user, category, and product
        jdbcTemplate.getJdbcOperations().execute("INSERT INTO users(username, password) VALUES('carol','pwd')");
        jdbcTemplate.getJdbcOperations()
                .execute("INSERT INTO categories(name, category_order) VALUES('existingCat', 1)");
        // insert a product
        productService.createProductAndOrLinkToUser("prod1", "carol");
        Set<ShoppingItemDomain> items = shoppingItemService.findAllByUsername("carol");
        ProductDomain item = items.stream().findFirst().map(ShoppingItemDomain::getProduct).orElseThrow();

        // update product name, rarity and assign existing category
        productService.updateProductCreateCategoryIfNecessary(new ProductDomain().setId((item.getId()))
                .setName("prod1Updated")
                .setIsRare(true)
                .setCategory(new CategoryDomain().setId(1L).setName("existingCat")));

        // verify update
        Set<ShoppingItemDomain> updated = shoppingItemService.findAllByUsername("carol");
        ProductDomain item2 = updated.stream().findFirst().map(ShoppingItemDomain::getProduct).orElseThrow();
        assertThat(item2.getName()).isEqualTo("prod1Updated");
        assertThat(item2.getIsRare()).isTrue();
        assertThat(item2.getCategory().getId()).isEqualTo(1);
    }

    @Test
    void updateProductWithNewCategory() {
        // seed user and initial product
        jdbcTemplate.getJdbcOperations().execute("INSERT INTO users(username, password) VALUES('dave','pwd')");
        productService.createProductAndOrLinkToUser("prod2", "dave");
        Set<ShoppingItemDomain> items = shoppingItemService.findAllByUsername("dave");
        ProductDomain item = items.stream().findFirst().map(ShoppingItemDomain::getProduct).orElseThrow();

        // update product, passing null categoryId to force new category creation
        productService.updateProductCreateCategoryIfNecessary(new ProductDomain().setId(item.getId())
                .setName("prod2Updated")
                .setIsRare(true)
                .setCategory(new CategoryDomain().setId(null).setName("newCat")));

        // verify that new category was created and assigned
        Set<ShoppingItemDomain> cats = shoppingItemService.findAllByUsername("dave");
        assertThat(cats).extracting(c -> c.getProduct().getCategory().getName()).contains("newCat");
        int newCatId = cats.stream()
                .filter(c -> "newCat".equals(c.getProduct().getCategory().getName()))
                .findFirst()
                .orElseThrow()
                .getProduct()
                .getCategory()
                .getOrder();
        Set<ShoppingItemDomain> updated = shoppingItemService.findAllByUsername("dave");
        assertThat(updated.stream()
                .findFirst()
                .map(si -> si.getProduct().getCategory().getId())
                .orElseThrow()).isEqualTo(newCatId);
    }

    @Test
    void updateAllProductQuantity() {
        // seed user, categories, and two products
        jdbcTemplate.getJdbcOperations().execute("INSERT INTO users(username, password) VALUES('eve','pwd')");
        jdbcTemplate.getJdbcOperations()
                .execute("INSERT INTO categories(name, category_order) VALUES('c1',1),('c2',2)");
        // insert two products for user
        productService.createProductAndOrLinkToUser("p1", "eve");
        productService.createProductAndOrLinkToUser("p2", "eve");
        // set non-zero quantities
        Set<ShoppingItemDomain> updatedItems = shoppingItemService.updateAllShoppingItemQuantities("eve", 7);
        assertThat(updatedItems).isNotEmpty().allMatch(i -> 7 == i.getQuantity());

        // call updateAllProductQuantity
        shoppingItemService.updateAllShoppingItemQuantities("eve", 0);
        Set<ShoppingItemDomain> reset = shoppingItemService.findAllByUsername("eve");
        assertThat(reset).isNotEmpty().allMatch(i -> 0 == i.getQuantity());
    }
}
