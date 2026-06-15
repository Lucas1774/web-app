package com.lucas.server.components.rubik.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity
@Table(name = "letter_pairs", indexes = @Index(columnList = "letter_pair"))
public class LetterPairs implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(name = "letter_pair", nullable = false, unique = true, length = 100)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String letterPair;

    @Column(name = "person", length = 100)
    private String person;

    @Column(name = "action", length = 100)
    private String action;

    @Column(name = "object", length = 10)
    private String object;
}
