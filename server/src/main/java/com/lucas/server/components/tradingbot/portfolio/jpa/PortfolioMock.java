package com.lucas.server.components.tradingbot.portfolio.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "portfolio_mock")
public class PortfolioMock extends PortfolioBase {
}
