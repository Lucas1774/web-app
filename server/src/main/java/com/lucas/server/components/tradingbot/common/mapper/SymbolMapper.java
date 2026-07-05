package com.lucas.server.components.tradingbot.common.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SymbolMapper implements EntityMapper<Symbol, SymbolDomain> {

    @Override
    public SymbolDomain toDto(Symbol entity) {
        if (null == entity) {
            return null;
        }
        return new SymbolDomain(entity.getId(), entity.getName(), entity.getSector());
    }

    @Override
    public Symbol toEntity(SymbolDomain dto) {
        if (null == dto) {
            return null;
        }
        return new Symbol().setId(dto.getId()).setName(dto.getName()).setSector(dto.getSector());
    }
}
