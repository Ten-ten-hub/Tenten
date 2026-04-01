package com.team.product_service.product.domain;

import com.team.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_product", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "name"}))
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "product_status")
    private ProductStatus status = ProductStatus.ON_SALE;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "description", length = 500)
    private String description;

    public static Product create(String name, UUID companyId, UUID hubId, BigDecimal unitPrice, String description) {
        return Product.builder()
            .name(name)
            .companyId(companyId)
            .hubId(hubId)
            .unitPrice(unitPrice)
            .description(description)
            .build();
    }

    public void update(String name, BigDecimal unitPrice, String description, ProductStatus status) {
        if (name != null) this.name = name;
        if (unitPrice != null) this.unitPrice = unitPrice;
        if (description != null) this.description = description;
        if (status != null) this.status = status;
    }

    @Override
    public void softDelete(UUID deletedBy) {
        super.softDelete(deletedBy);
        this.status = ProductStatus.DISCONTINUED;
    }

}
