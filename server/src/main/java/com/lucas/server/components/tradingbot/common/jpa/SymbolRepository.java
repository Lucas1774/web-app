package com.lucas.server.components.tradingbot.common.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface SymbolRepository extends JpaRepository<Symbol, Long> {

    Set<Symbol> findByNameIn(Set<String> names);
}
