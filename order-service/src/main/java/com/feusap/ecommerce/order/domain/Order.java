package com.feusap.ecommerce.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapped under the entity name {@code PurchaseOrder} because {@code Order} is a reserved word in JPQL.
 */
@Entity(name = "PurchaseOrder")
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    private Order(Long customerId) {
        this.customerId = customerId;
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
        this.createdAt = Instant.now();
    }

    public static Order forCustomer(Long customerId) {
        return new Order(customerId);
    }

    public void addLine(OrderLine line) {
        line.attachTo(this);
        lines.add(line);
        totalAmount = totalAmount.add(line.getLineTotal());
    }

    public void confirm() {
        if (lines.isEmpty()) {
            throw new IllegalStateException("An order without lines cannot be confirmed");
        }
        status = OrderStatus.CONFIRMED;
    }
    public void markPaymentPending() {          // renomme toPay()
        if (lines.isEmpty()) {
            throw new IllegalStateException("An order without lines cannot be placed");
        }
        status = OrderStatus.PAYMENT_PENDING;
    }

    public void cancel() {
        status = OrderStatus.CANCELLED;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
