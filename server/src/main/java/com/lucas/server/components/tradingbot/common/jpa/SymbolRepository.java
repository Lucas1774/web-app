package com.lucas.server.components.tradingbot.common.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface SymbolRepository extends JpaRepository<Symbol, Long> {

    Collection<Symbol> findByNameIn(Collection<String> names);
}
