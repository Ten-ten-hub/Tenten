package com.team.userservice.user.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "p_company_user")
@Getter
@NoArgsConstructor
public class CompanyUser extends BaseEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @JoinColumn(name = "user_id", unique = true, nullable = false) // 물리 FK
    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(name = "company_id", nullable = false) //논리 FK // 유니크일 필요 없음
    private UUID companyId;

    private CompanyUser(User user, UUID companyId) {
        this.user = user;
        this.companyId = companyId;
    }

    public static CompanyUser create(User user, UUID companyId) {
        return new CompanyUser(user, companyId);
    }

    public void updateCompanyId(UUID companyId) {
        this.companyId = companyId;
    }
}
