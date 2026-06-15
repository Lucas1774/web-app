package com.lucas.server.components.calculator.dto;

import com.lucas.server.common.dto.DomainEntity;
import lombok.AllArgsConstructor;
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
@Accessors(chain = true)
public class CalculatorDomain implements DomainEntity {
    @ToString.Include
    private Long id;
    @ToString.Include
    private String ans;
    @ToString.Include
    private String text;
    @ToString.Include
    private Boolean textMode;
}
