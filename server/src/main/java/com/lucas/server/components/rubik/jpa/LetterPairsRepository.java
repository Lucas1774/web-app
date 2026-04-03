package com.lucas.server.components.rubik.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface LetterPairsRepository extends JpaRepository<LetterPairs, Long> {

    Optional<LetterPairs> findByLetterPair(String letterPair);

    Set<LetterPairs> findByLetterPairIn(Set<String> letterPairs);
}
