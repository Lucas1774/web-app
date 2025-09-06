package com.lucas.server.components.tradingbot.common.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SymbolRepository extends JpaRepository<Symbol, Long> {

    List<Symbol> findByNameIn(Collection<String> names);
}
