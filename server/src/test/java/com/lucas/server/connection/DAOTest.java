package com.lucas.server.connection;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.components.shopping.dto.Category;
import com.lucas.server.components.shopping.dto.ShoppingItem;
import com.lucas.server.components.sudoku.Sudoku;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DAOTest {
    @Autowired
    DAO dao;

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.getJdbcOperations().execute(
                "TRUNCATE TABLE shopping, products, categories, sudokus, users RESTART IDENTITY CASCADE"
        );
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO users(username, password) VALUES ('admin','admin'), ('default','default')"
        );
    }

    @Test
    void testShoppingCRUD() {
        // seed user and category
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO users(username, password) VALUES('bob','pwd')"
        );
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO categories(name, category_order) VALUES('catA',1)"
        );

        // insert product and assign
        dao.insertProduct("item1", "bob");
        List<ShoppingItem> items = dao.getShoppingItems("bob");
        assertThat(items).hasSize(1);
        ShoppingItem item = items.getFirst();
        assertThat(item.getName()).isEqualTo("item1");

        // update quantity
        dao.updateProductQuantity(item.getId(), 5, "bob");
        items = dao.getShoppingItems("bob");
        assertThat(items.getFirst().getQuantity()).isEqualTo(5);

        // remove
        dao.removeProduct(item.getId(), "bob");
        items = dao.getShoppingItems("bob");
        assertThat(items).isEmpty();
    }

    @Test
    void testInsertAndRetrieveSudoku() {
        Sudoku s1 = Sudoku.withDefaultValues();
        dao.insertSudokus(List.of(s1));
        List<Sudoku> out = dao.getSudokus();
        assertThat(out.getFirst().get()).isEqualTo(s1.get());
    }

    @Test
    void testGetPossibleCategories() {
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO categories(name, category_order) VALUES('x',10),( 'y',20 )"
        );
        List<Category> cats = dao.getPossibleCategories();
        assertThat(cats).extracting(Category::getName).containsExactly("x", "y");
    }

    @Test
    void testUpdateOrders() {
        // seed categories
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO categories(name, category_order) VALUES('a',1),('b',2)"
        );
        List<Category> cats = dao.getPossibleCategories();
        // swap order
        Category first = cats.get(0);
        Category second = cats.get(1);
        first.setOrder(second.getOrder());
        second.setOrder(first.getOrder() - 1);

        dao.updateOrders(List.of(first, second));
        List<Category> updated = dao.getPossibleCategories();
        assertThat(updated.getFirst().getOrder()).isEqualTo(second.getOrder());
    }

    @Test
    void testUpdateProductWithExistingCategory() {
        // seed user, category, and product
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO users(username, password) VALUES('carol','pwd')"
        );
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO categories(name, category_order) VALUES('existingCat', 1)"
        );
        // insert a product
        dao.insertProduct("prod1", "carol");
        List<ShoppingItem> items = dao.getShoppingItems("carol");
        int prodId = items.getFirst().getId();

        // update product name, rarity and assign existing category
        dao.updateProduct(prodId, "prod1Updated", true, 1, "existingCat");

        // verify update
        List<ShoppingItem> updated = dao.getShoppingItems("carol");
        assertThat(updated.getFirst().getName()).isEqualTo("prod1Updated");
        assertThat(updated.getFirst().getIsRare()).isTrue();
        assertThat(updated.getFirst().getCategoryId()).isEqualTo(1);
    }

    @Test
    void testUpdateProductWithNewCategory() {
        // seed user and initial product
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO users(username, password) VALUES('dave','pwd')"
        );
        dao.insertProduct("prod2", "dave");
        List<ShoppingItem> items = dao.getShoppingItems("dave");
        int prodId = items.getFirst().getId();

        // update product, passing null categoryId to force new category creation
        dao.updateProduct(prodId, "prod2Updated", false, null, "newCat");

        // verify that new category was created and assigned
        List<Category> cats = dao.getPossibleCategories();
        assertThat(cats).extracting(Category::getName).contains("newCat");
        int newCatId = cats.stream()
                .filter(c -> "newCat".equals(c.getName()))
                .findFirst().orElseThrow().getOrder();
        List<ShoppingItem> updated = dao.getShoppingItems("dave");
        assertThat(updated.getFirst().getCategoryId()).isEqualTo(newCatId);
    }

    @Test
    void testUpdateAllProductQuantity() {
        // seed user, categories, and two products
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO users(username, password) VALUES('eve','pwd')"
        );
        jdbcTemplate.getJdbcOperations().execute(
                "INSERT INTO categories(name, category_order) VALUES('c1',1),('c2',2)"
        );
        // insert two products for user
        dao.insertProduct("p1", "eve");
        dao.insertProduct("p2", "eve");
        List<ShoppingItem> items = dao.getShoppingItems("eve");
        // set non-zero quantities
        for (ShoppingItem it : items) {
            dao.updateProductQuantity(it.getId(), 7, "eve");
        }
        // verify quantities updated
        items = dao.getShoppingItems("eve");
        assertThat(items).allMatch(i -> i.getQuantity() == 7);

        // call updateAllProductQuantity
        dao.updateAllProductQuantity("eve");
        List<ShoppingItem> reset = dao.getShoppingItems("eve");
        assertThat(reset).isNotEmpty().allMatch(i -> i.getQuantity() == 0);
    }
}
