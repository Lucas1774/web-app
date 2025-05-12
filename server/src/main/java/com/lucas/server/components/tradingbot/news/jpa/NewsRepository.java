package com.lucas.server.components.tradingbot.news.jpa;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    Slice<News> findBySymbol_Id(Long symbolId, PageRequest publicationDate);

    Optional<News> findByExternalId(Long externalId);
}
