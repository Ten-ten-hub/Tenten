package com.team.userservice.user.core;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
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
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.UuidGenerator;

@Slf4j
@Entity
@Getter
@NoArgsConstructor
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
    private String deletedBy; // 레코드 삭제자

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

    public void register(Role role) {
        if (this.signupStatus == SignupStatus.APPROVED) {
            throw new UserException(UserErrorCode.ALREADY_REGISTERED_USER);
        }
        this.signupStatus = SignupStatus.APPROVED;
        this.role = role;
    }
}

