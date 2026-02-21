package com.lucas.server.components.shopping.jpa.category;

import com.lucas.server.ConfiguredTest;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CategoryJpaServiceTest extends ConfiguredTest {

    @Autowired
    private CategoryJpaService categoryService;

    @Test
    @Transactional
    void findAllByOrderByOrderAsc() {
        // given
        Category category1 = new Category()
                .setName("x")
                .setOrder(10);
        Category category2 = new Category()
                .setName("y")
                .setOrder(20);
        categoryService.createAll(Set.of(category2, category1));

        // when
        OrderedIndexedSet<Category> result = categoryService.findAllByOrderByOrderAsc();

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
    @Transactional
    void updateOrders() {
        // given: two categories with reversed order values
        Category c1 = new Category().setName("A").setOrder(2);
        Category c2 = new Category().setName("B").setOrder(1);
        Set<Category> saved = categoryService.createAll(Set.of(c1, c2));
        assertThat(saved)
                .extracting(Category::getName, Category::getOrder)
                .containsExactlyInAnyOrder(
                        tuple("A", 2),
                        tuple("B", 1)
                );

        // when: updating with "A" first "B" second
        OrderedIndexedSet<Category> input = saved.stream().sorted(Comparator.comparing(Category::getOrder).reversed())
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
        Set<Category> result = categoryService.updateOrders(input);

        // then: orders should be reassigned to [1,2]
        assertThat(result)
                .extracting(Category::getName, Category::getOrder)
                .containsExactlyInAnyOrder(
                        tuple("A", 1),
                        tuple("B", 2)
                );
    }
}
