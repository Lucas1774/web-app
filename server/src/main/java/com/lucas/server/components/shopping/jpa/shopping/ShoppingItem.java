package com.lucas.server.components.shopping.jpa.shopping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.common.jpa.user.User;
import com.lucas.server.components.shopping.jpa.product.Product;
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
@Table(name = "shopping", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class ShoppingItem implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Override
    public String toString() {
        return "Shopping{" +
                "id=" + id +
                ", product=" + product +
                '}';
    }
}
