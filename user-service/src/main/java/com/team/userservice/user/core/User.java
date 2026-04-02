package com.team.userservice.user.core;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
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

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt; //TODO: api 명세서에 업데이트하는 api 추가해야함

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 레코드 삭제 시간

    @Column(name = "deleted_by", length = 100)
    private UUID deletedBy; // 레코드 삭제자

    @Builder(builderMethodName = "create")
    private User(String loginId, String password, String name, Role role,
                 String slackId, String email, String phoneNumber) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.role = role;
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

    public void deleteUser(UUID deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;

    }

    public void updateLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}

