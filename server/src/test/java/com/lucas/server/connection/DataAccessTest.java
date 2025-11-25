package com.lucas.server.connection;

import com.lucas.server.ConfiguredTest;
import com.lucas.server.components.shopping.jpa.category.Category;
import com.lucas.server.components.shopping.jpa.category.CategoryJpaService;
import com.lucas.server.components.shopping.jpa.product.Product;
import com.lucas.server.components.shopping.jpa.product.ProductJpaService;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItem;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItemJpaService;
import com.lucas.server.components.sudoku.jpa.Sudoku;
import com.lucas.server.components.sudoku.jpa.SudokuJpaService;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.getJdbcOperations().execute(
                "TRUNCATE TABLE shopping, products, categories, sudokus, users RESTART IDENTITY CASCADE"
        );
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO users(username, password) VALUES ('admin','admin'), ('default','default')"
        );
    }

    @Test
    @Transactional
    void testShoppingCRUD() {
        // seed user and category
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO users(username, password) VALUES('bob','pwd')"
        );
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO categories(name, category_order) VALUES('catA',1)"
        );

        // insert product and assign
        productService.createProductAndOrLinkToUser("item1", "bob");
        Set<ShoppingItem> items = shoppingItemService.findAllByUsername("bob");
        assertThat(items).hasSize(1);
        ShoppingItem item = items.stream().findFirst().orElseThrow();
        assertThat(item.getProduct().getName()).isEqualTo("item1");

        // update quantity
        assertThat(shoppingItemService.updateShoppingItemQuantity(new ShoppingItem().setProduct(item.getProduct()).setQuantity(5), "bob").getQuantity()).isEqualTo(5);

        // remove
        shoppingItemService.deleteByProductAndUsernameRemoveOrphanedProductIfNecessary(item.getProduct(), "bob");
        items = shoppingItemService.findAllByUsername("bob");
        assertThat(items).isEmpty();
    }

    @Test
    @Transactional
    void testInsertAndRetrieveSudoku() {
        Sudoku s1 = Sudoku.withDefaultValues();
        sudokuService.createIgnoringDuplicates(Collections.singleton(s1));
        assertThat(sudokuService.findAll())
                .hasSize(1)
                .extracting(Sudoku::getState)
                .containsExactly(s1.getState());
    }

    @Test
    @Transactional
    void testGetPossibleCategories() {
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO categories(name, category_order) VALUES('x',10),( 'y',20 )"
        );
        Set<Category> cats = categoryService.findAllByOrderByOrderAsc();
        assertThat(cats).extracting(Category::getName).containsExactly("x", "y");
    }

    @Test
    @Transactional
    void testUpdateOrders() {
        // seed categories
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO categories(name, category_order) VALUES('a',1),('b',2)"
        );
        OrderedIndexedSet<Category> cats = categoryService.findAllByOrderByOrderAsc();
        // swap order
        Category first = cats.get(0);
        Category second = cats.get(1);

        categoryService.updateOrders(OrderedIndexedSet.of(second, first));
        Set<Category> updated = categoryService.findAllByOrderByOrderAsc();
        assertThat(updated)
                .extracting(Category::getName, Category::getOrder)
                .containsExactlyInAnyOrder(
                        tuple("b", 1),
                        tuple("a", 2)
                );
    }

    @Test
    @Transactional
    void testUpdateProductWithExistingCategory() {
        // seed user, category, and product
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO users(username, password) VALUES('carol','pwd')"
        );
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO categories(name, category_order) VALUES('existingCat', 1)"
        );
        // insert a product
        productService.createProductAndOrLinkToUser("prod1", "carol");
        Set<ShoppingItem> items = shoppingItemService.findAllByUsername("carol");
        Product item = items.stream().findFirst().map(ShoppingItem::getProduct).orElseThrow();

        // update product name, rarity and assign existing category
        productService.updateProductCreateCategoryIfNecessary(new Product().setId((item.getId())).setName("prod1Updated").setIsRare(true).setCategory(
                new Category().setId(1L).setName("existingCat")
        ));

        // verify update
        Set<ShoppingItem> updated = shoppingItemService.findAllByUsername("carol");
        Product item2 = updated.stream().findFirst().map(ShoppingItem::getProduct).orElseThrow();
        assertThat(item2.getName()).isEqualTo("prod1Updated");
        assertThat(item2.getIsRare()).isTrue();
        assertThat(item2.getCategory().getId()).isEqualTo(1);
    }

    @Test
    @Transactional
    void testUpdateProductWithNewCategory() {
        // seed user and initial product
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO users(username, password) VALUES('dave','pwd')"
        );
        productService.createProductAndOrLinkToUser("prod2", "dave");
        Set<ShoppingItem> items = shoppingItemService.findAllByUsername("dave");
        Product item = items.stream().findFirst().map(ShoppingItem::getProduct).orElseThrow();

        // update product, passing null categoryId to force new category creation
        productService.updateProductCreateCategoryIfNecessary(
                new Product().setId(item.getId()).setName("prod2Updated").setIsRare(true).setCategory(
                        new Category().setId(null).setName("newCat")
                )
        );

        // verify that new category was created and assigned
        Set<ShoppingItem> cats = shoppingItemService.findAllByUsername("dave");
        assertThat(cats).extracting(c -> c.getProduct().getCategory().getName()).contains("newCat");
        int newCatId = cats.stream()
                .filter(c -> "newCat".equals(c.getProduct().getCategory().getName()))
                .findFirst().orElseThrow().getProduct().getCategory().getOrder();
        Set<ShoppingItem> updated = shoppingItemService.findAllByUsername("dave");
        assertThat(
                updated.stream().findFirst().map(s -> s.getProduct().getCategory().getId()).orElseThrow()
        ).isEqualTo(newCatId);
    }

    @Test
    @Transactional
    void testUpdateAllProductQuantity() {
        // seed user, categories, and two products
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO users(username, password) VALUES('eve','pwd')"
        );
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO categories(name, category_order) VALUES('c1',1),('c2',2)"
        );
        // insert two products for user
        productService.createProductAndOrLinkToUser("p1", "eve");
        productService.createProductAndOrLinkToUser("p2", "eve");
        // set non-zero quantities
        Set<ShoppingItem> updatedItems = shoppingItemService.updateAllShoppingItemQuantities("eve", 7);
        assertThat(updatedItems).isNotEmpty().allMatch(i -> 7 == i.getQuantity());

        // call updateAllProductQuantity
        shoppingItemService.updateAllShoppingItemQuantities("eve", 0);
        Set<ShoppingItem> reset = shoppingItemService.findAllByUsername("eve");
        assertThat(reset).isNotEmpty().allMatch(i -> 0 == i.getQuantity());
    }
}
