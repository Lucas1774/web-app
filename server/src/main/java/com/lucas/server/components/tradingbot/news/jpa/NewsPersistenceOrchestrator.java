package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Small coordinator that acquires a JVM-wide lock before delegating
 * to the transactional NewsJpaService.
 */
@Service
@RequiredArgsConstructor
public class NewsPersistenceOrchestrator {

    private final NewsJpaService newsJpaService;

    public synchronized Set<NewsDomain> persistNews(Set<NewsDomain> entities) {
        if (entities.isEmpty()) {
            return entities;
        }
        return newsJpaService.createOrUpdate(entities);
    }
}
