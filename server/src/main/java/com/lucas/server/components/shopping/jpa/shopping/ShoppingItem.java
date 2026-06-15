package com.lucas.server.components.shopping.jpa.shopping;

import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.common.jpa.user.User;
import com.lucas.server.components.shopping.jpa.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "shopping", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class ShoppingItem implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Product product;

    @Column(nullable = false)
    @ToString.Include
    private Integer quantity;
}
