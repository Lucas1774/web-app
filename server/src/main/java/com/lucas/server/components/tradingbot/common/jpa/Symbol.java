package com.lucas.server.components.tradingbot.common.jpa;

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
@Table(name = "symbol")
public class Symbol implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String name;

    @Override
    public String toString() {
        return "Symbol{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return Objects.equals(id, symbol.id) && Objects.equals(name, symbol.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
