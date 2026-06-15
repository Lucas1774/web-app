package com.lucas.server.components.calculator.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.calculator.dto.CalculatorDomain;
import com.lucas.server.components.calculator.jpa.Calculator;
import org.springframework.stereotype.Component;

import static java.lang.Boolean.TRUE;

@Component
public class CalculatorMapper implements EntityMapper<Calculator, CalculatorDomain> {

    @Override
    public CalculatorDomain toDto(Calculator entity) {
        if (null == entity) {
            return null;
        }
        return new CalculatorDomain(entity.getId(), entity.getAns(), entity.getText(), entity.isTextMode());
    }

    @Override
    public Calculator toEntity(CalculatorDomain dto) {
        if (null == dto) {
            return null;
        }
        return new Calculator().setId(dto.getId())
                .setAns(dto.getAns())
                .setText(dto.getText())
                .setTextMode(TRUE.equals(dto.getTextMode()));
    }
}
