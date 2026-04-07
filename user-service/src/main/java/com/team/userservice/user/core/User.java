package com.team.userservice.user.core;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.core.vo.UserUpdateInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;
import com.team.common.BaseEntity;

@Entity
@Getter
@NoArgsConstructor
@SQLRestriction("deleted_at IS NULL") //User를 조회하는 모든 쿼리에 자동으로 붙는 조건 : 삭제 처리된 유저 조회 x
@Table(name = "p_user")
public class User extends BaseEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "login_id", unique = true, nullable = false)
    private String loginId;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "slack_id", unique = true, nullable = false)
    private String slackId;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(name = "signup_status")
    @Enumerated(EnumType.STRING)
    private SignupStatus signupStatus;

    @Column(name = "affiliated_status")
    @Enumerated(EnumType.STRING)
    private AffiliatedStatus affiliatedStatus;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder(builderMethodName = "create")
    private User(String loginId, String password, String name,
                 String slackId, String email, String phoneNumber) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.role = Role.NONE;
        this.affiliatedStatus = AffiliatedStatus.NOT_APPLICABLE;
        this.slackId = slackId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.signupStatus = SignupStatus.PENDING;
    }

    public void register() {
        if (this.signupStatus == SignupStatus.APPROVED) {
            throw new UserException(UserErrorCode.ALREADY_REGISTERED_USER);
        }
        this.signupStatus = SignupStatus.APPROVED;
    }

    public void updateRole(Role role) {
        boolean wasHub = this.role == Role.HUB_ADMIN || this.role == Role.HUB_DELIVERY_MANAGER;
        boolean wasCompany = this.role == Role.COMPANY_MANAGER || this.role == Role.COM_DELIVERY_MANAGER;
        boolean toHub = role == Role.HUB_ADMIN || role == Role.HUB_DELIVERY_MANAGER;
        boolean toCompany = role == Role.COMPANY_MANAGER || role == Role.COM_DELIVERY_MANAGER;

        if((wasHub && toCompany) || (wasCompany && toHub)){
            this.affiliatedStatus = AffiliatedStatus.UNAFFILIATED;
        }

        if(role == Role.NONE || role == Role.MASTER_ADMIN){
            this.affiliatedStatus = AffiliatedStatus.NOT_APPLICABLE;
        }else if(this.affiliatedStatus == AffiliatedStatus.NOT_APPLICABLE){
            this.affiliatedStatus = AffiliatedStatus.UNAFFILIATED;
        }

        this.role = role;
    }

    public void userUpdate(UserUpdateInfo updateInfo) {
        if (updateInfo.loginId() != null) {
            this.loginId = updateInfo.loginId();
        }
        if (updateInfo.name() != null) {
            this.name = updateInfo.name();
        }
        if (updateInfo.slackId() != null) {
            this.slackId = updateInfo.slackId();
        }
        if (updateInfo.email() != null) {
            this.email = updateInfo.email();
        }
        if (updateInfo.phoneNumber() != null) {
            this.phoneNumber = updateInfo.phoneNumber();
        }
    }

    public void updateLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void updateUserAffiliation(AffiliatedStatus affiliatedStatus) {
        this.affiliatedStatus = affiliatedStatus;
    }
}

