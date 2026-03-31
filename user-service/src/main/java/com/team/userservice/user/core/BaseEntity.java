package com.team.userservice.user.core;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 레코드 생성 시간

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy; // 레코드 생성자

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 레코드 수정 시간

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private UUID updatedBy; // 레코드 수정자


}
