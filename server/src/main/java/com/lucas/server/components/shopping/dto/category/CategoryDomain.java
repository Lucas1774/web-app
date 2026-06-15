package com.lucas.server.components.shopping.dto.category;

import com.lucas.server.common.dto.DomainEntity;
import com.lucas.server.components.shopping.dto.Sortable;
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
public class CategoryDomain implements Sortable, DomainEntity {
    @ToString.Include
    private Long id;
    @EqualsAndHashCode.Include
    @ToString.Include
    private String name;
    @ToString.Include
    private Integer order;
}
