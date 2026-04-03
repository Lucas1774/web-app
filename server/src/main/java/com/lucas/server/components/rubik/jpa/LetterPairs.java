package com.lucas.server.components.rubik.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "letter_pairs", indexes = @Index(columnList = "letter_pair"))
public class LetterPairs implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "letter_pair", length = 100, unique = true)
    private String letterPair;

    @Column(name = "person", length = 100)
    private String person;

    @Column(name = "action", length = 100)
    private String action;

    @Column(name = "object", length = 10)
    private String object;

    @Override
    public boolean equals(Object o) {
        if (null == o || getClass() != o.getClass()) return false;
        LetterPairs that = (LetterPairs) o;
        return Objects.equals(letterPair, that.letterPair);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(letterPair);
    }
}
