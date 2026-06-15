package com.lucas.server.components.calculator.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
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
@Accessors(chain = true)
@Entity
@Table(name = "my_table")
public class Calculator implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(name = "ans", length = 50)
    @ToString.Include
    private String ans;

    @Column(name = "text")
    @ToString.Include
    private String text;

    @Column(name = "text_mode", nullable = false)
    @ToString.Include
    private boolean textMode;
}
