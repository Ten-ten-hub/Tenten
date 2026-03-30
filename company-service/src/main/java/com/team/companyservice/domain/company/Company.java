package com.team.companyservice.domain.company;

import java.util.UUID;

import com.team.companyservice.domain.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "p_company")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false)
    private CompanyType companyType;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(length = 20)
    private String zipcode;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "contact_slack_id", length = 100)
    private String contactSlackId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Builder
    private Company(
            UUID id,
            String name,
            CompanyType companyType,
            UUID hubId,
            String address,
            String addressDetail,
            String zipcode,
            String contactName,
            String contactPhone,
            String contactSlackId,
            boolean isActive
    ) {
        this.id = id;
        this.name = name;
        this.companyType = companyType;
        this.hubId = hubId;
        this.address = address;
        this.addressDetail = addressDetail;
        this.zipcode = zipcode;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactSlackId = contactSlackId;
        this.isActive = isActive;
    }

    public static Company create(
            String name,
            CompanyType companyType,
            UUID hubId,
            String address,
            String addressDetail,
            String zipcode,
            String contactName,
            String contactPhone,
            String contactSlackId,
            UUID createdBy
    ) {
        Company company = Company.builder()
                .id(UUID.randomUUID())
                .name(name)
                .companyType(companyType)
                .hubId(hubId)
                .address(address)
                .addressDetail(addressDetail)
                .zipcode(zipcode)
                .contactName(contactName)
                .contactPhone(contactPhone)
                .contactSlackId(contactSlackId)
                .isActive(true)
                .build();

        company.markCreated(createdBy);
        return company;
    }

    public void update(
            String name,
            CompanyType companyType,
            UUID hubId,
            String address,
            String addressDetail,
            String zipcode,
            String contactName,
            String contactPhone,
            String contactSlackId,
            UUID updatedBy
    ) {
        this.name = name;
        this.companyType = companyType;
        this.hubId = hubId;
        this.address = address;
        this.addressDetail = addressDetail;
        this.zipcode = zipcode;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactSlackId = contactSlackId;
        this.markUpdated(updatedBy);
    }

    public void softDelete(UUID deletedBy) {
        this.isActive = false;
        this.markDeleted(deletedBy);
    }
}