package com.lucas.server.common.jpa;

import com.lucas.server.common.jpa.user.OrderColumnServiceDelegate;
import com.lucas.server.components.shopping.dto.Sortable;
import com.lucas.utils.OrderedIndexedSet;

import java.util.Set;

/**
 * Interface to update the order of entities in a list.
 * Ideally, classes implementing this interface will do it through a {@link OrderColumnServiceDelegate}
 *
 * @param <T> the type of the entities to be sorted
 */
public interface OrderColumnJpaService<T extends Sortable> {

    /**
     * @param newlySortedElements the entities to be sorted. Ideally must exist
     * @return the sorted entities
     */
    Set<T> updateOrders(OrderedIndexedSet<T> newlySortedElements);
}
