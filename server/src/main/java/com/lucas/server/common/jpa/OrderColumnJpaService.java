package com.lucas.server.common.jpa;

import com.lucas.server.components.shopping.dto.Sortable;

import java.util.List;

public interface OrderColumnJpaService<T extends Sortable> {

    List<T> updateOrders(List<T> newlySortedElements);
}
