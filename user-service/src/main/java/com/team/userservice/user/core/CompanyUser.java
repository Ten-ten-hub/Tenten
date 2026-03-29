package com.team.userservice.user.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "p_company_user")
public class CompanyUser extends BaseEntity{
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @JoinColumn(name = "user_id", unique = true, nullable = false) // 물리 FK
    @OneToOne(fetch = FetchType.LAZY)
    private User userId;

    @Column(name = "company_id", nullable = false) //논리 FK // 유니크일 필요 없음
    private UUID companyId;
}
