package com.lucas.server.components.calculator.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "my_table")
public class Calculator implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ans", length = 50)
    private String ans;

    @Column(name = "text")
    private String text;

    @Column(name = "text_mode", nullable = false)
    private boolean textMode = false;

    @Override
    public String toString() {
        return "Calculator{" +
                "id=" + id +
                ", ans='" + ans + '\'' +
                ", text='" + text + '\'' +
                ", textMode=" + textMode +
                '}';
    }
}
