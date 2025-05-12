package com.lucas.server.components.shopping.jpa.category;

import com.lucas.server.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CategoryJpaServiceTest {

    @Autowired
    CategoryJpaService categoryService;

    @BeforeEach
    void setup() {
        categoryService.deleteAll();
    }

    @Test
    void testFindAllByOrderByOrderAsc() {
        // given
        Category category1 = new Category()
                .setName("x")
                .setOrder(10);
        Category category2 = new Category()
                .setName("y")
                .setOrder(20);
        categoryService.createAll(List.of(category2, category1));

        // when
        List<Category> result = categoryService.findAllByOrderByOrderAsc();

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(Category::getOrder)
                .containsExactly(10, 20);

        assertThat(result)
                .extracting(Category::getName, Category::getOrder)
                .containsExactly(
                        tuple("x", 10),
                        tuple("y", 20)
                );
    }

    @Test
    void testUpdateOrders() {
        // given: two categories with reversed order values
        Category c1 = new Category().setName("A").setOrder(2);
        Category c2 = new Category().setName("B").setOrder(1);
        List<Category> saved = categoryService.createAll(Arrays.asList(c1, c2));
        assertThat(saved)
                .extracting(Category::getOrder)
                .containsExactly(2, 1);

        // when:
        List<Category> input = Arrays.asList(saved.get(0), saved.get(1));
        Collections.reverse(input);
        List<Category> result = categoryService.updateOrders(input);

        // then: orders should be reassigned to [1,2]
        assertThat(result)
                .extracting(Category::getOrder)
                .containsExactly(1, 2);

        assertThat(result)
                .extracting(Category::getName, Category::getOrder)
                .containsExactly(
                        tuple(saved.get(1).getName(), 1),
                        tuple(saved.get(0).getName(), 2)
                );
    }
}
