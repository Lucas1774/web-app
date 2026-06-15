package com.lucas.server.components.shopping.jpa.category;

import com.lucas.server.ConfiguredTest;
import com.lucas.server.components.shopping.dto.category.CategoryDomain;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
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
    void findAllByOrderByOrderAsc() {
        // given
        CategoryDomain category1 = new CategoryDomain().setName("x").setOrder(10);
        CategoryDomain category2 = new CategoryDomain().setName("y").setOrder(20);
        categoryService.saveAll(Set.of(category2, category1));

        // when
        OrderedIndexedSet<CategoryDomain> result = categoryService.findAllByOrderByOrderAsc();

        // then
        assertThat(result).hasSize(2).extracting(CategoryDomain::getOrder).containsExactly(10, 20);

        assertThat(result).extracting(CategoryDomain::getName, CategoryDomain::getOrder)
                .containsExactly(tuple("x", 10), tuple("y", 20));
    }

    @Test
    void updateOrders() {
        // given: two categories with reversed order values
        CategoryDomain c1 = new CategoryDomain().setName("A").setOrder(2);
        CategoryDomain c2 = new CategoryDomain().setName("B").setOrder(1);
        Set<CategoryDomain> saved = categoryService.saveAll(Set.of(c1, c2));
        assertThat(saved).extracting(CategoryDomain::getName, CategoryDomain::getOrder)
                .containsExactlyInAnyOrder(tuple("A", 2), tuple("B", 1));

        // when: updating with "A" first "B" second
        OrderedIndexedSet<CategoryDomain> input = saved.stream()
                .sorted(Comparator.comparing(CategoryDomain::getOrder).reversed())
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
        Set<CategoryDomain> result = categoryService.updateOrders(input);

        // then: orders should be reassigned to [1,2]
        assertThat(result).extracting(CategoryDomain::getName, CategoryDomain::getOrder)
                .containsExactlyInAnyOrder(tuple("A", 1), tuple("B", 2));
    }
}
