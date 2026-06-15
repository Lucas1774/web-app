package com.lucas.server.components.shopping.dto.shopping;

import com.lucas.server.common.dto.DomainEntity;
import com.lucas.server.common.dto.user.UserDomain;
import com.lucas.server.components.shopping.dto.product.ProductDomain;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
public class ShoppingItemDomain implements DomainEntity {
    @ToString.Include
    private Long id;
    @EqualsAndHashCode.Include
    @ToString.Include
    private UserDomain user;
    @EqualsAndHashCode.Include
    @ToString.Include
    private ProductDomain product;
    @ToString.Include
    private Integer quantity;
}
