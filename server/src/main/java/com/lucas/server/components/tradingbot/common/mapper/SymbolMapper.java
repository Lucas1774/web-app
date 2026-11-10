package com.lucas.server.components.tradingbot.common.mapper;

import com.lucas.server.common.Sp500Symbols;
import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.lucas.server.common.Sp500Symbols.SYMBOL_TO_SECTOR;

@Component
@RequiredArgsConstructor
public class SymbolMapper implements EntityMapper<Symbol, SymbolDomain> {

    @Override
    public SymbolDomain toDto(Symbol entity) {
        if (null == entity) {
            return null;
        }
        Sp500Symbols.Sector sector = entity.getSector();
        if (null == sector && null != entity.getName()) {
            sector = SYMBOL_TO_SECTOR.get(entity.getName());
        }
        return new SymbolDomain(entity.getId(), entity.getName(), sector);
    }

    @Override
    public Symbol toEntity(SymbolDomain dto) {
        if (null == dto) {
            return null;
        }
        return new Symbol().setId(dto.getId()).setName(dto.getName()).setSector(dto.getSector());
    }
}
