package com.lucas.server.components.rubik.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface AlgorithmMappingRepository extends JpaRepository<AlgorithmMapping, Long> {

    Optional<AlgorithmMapping> findByFirstStickerAndSecondSticker(Integer firstSticker, Integer secondSticker);

    Set<AlgorithmMapping> findByFirstStickerInAndSecondStickerIn(Set<Integer> firstStickers, Set<Integer> secondStickers);
}
