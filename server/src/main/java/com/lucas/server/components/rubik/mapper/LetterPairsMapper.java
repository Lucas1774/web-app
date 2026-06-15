package com.lucas.server.components.rubik.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.rubik.dto.LetterPairsDomain;
import com.lucas.server.components.rubik.jpa.LetterPairs;
import org.springframework.stereotype.Component;

@Component
public class LetterPairsMapper implements EntityMapper<LetterPairs, LetterPairsDomain> {

    @Override
    public LetterPairsDomain toDto(LetterPairs entity) {
        if (null == entity) {
            return null;
        }
        return new LetterPairsDomain(entity.getId(),
                entity.getLetterPair(),
                entity.getPerson(),
                entity.getAction(),
                entity.getObject());
    }

    @Override
    public LetterPairs toEntity(LetterPairsDomain dto) {
        if (null == dto) {
            return null;
        }
        return new LetterPairs().setId(dto.getId())
                .setLetterPair(dto.getLetterPair())
                .setPerson(dto.getPerson())
                .setAction(dto.getAction())
                .setObject(dto.getObject());
    }
}
