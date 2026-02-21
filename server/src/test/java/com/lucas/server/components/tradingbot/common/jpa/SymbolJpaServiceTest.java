package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.ConfiguredTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SymbolJpaServiceTest extends ConfiguredTest {

    @Autowired
    private SymbolJpaService service;

    @Test
    void getOrCreateByName() {
        // when
        Set<Symbol> symbols = service.getOrCreateByName(Set.of("AAPL", "MSFT"));

        // then
        assertThat(symbols).hasSize(2);
        assertThat(symbols).extracting(Symbol::getName).containsExactlyInAnyOrder("AAPL", "MSFT");
        assertThat(symbols).allMatch(s -> null != s.getSector());
    }

    @Test
    void findById() {
        // given
        Symbol created = service.getOrCreateByName(Set.of("AAPL")).iterator().next();

        // when
        Optional<Symbol> found = service.findById(created.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("AAPL");
    }

    @Test
    void findAllById() {
        // given
        Set<Symbol> created = service.getOrCreateByName(Set.of("AAPL", "MSFT"));
        Set<Long> ids = Set.of(created.iterator().next().getId());

        // when
        Set<Symbol> found = service.findAllById(ids);

        // then
        assertThat(found).hasSize(1);
    }
}
