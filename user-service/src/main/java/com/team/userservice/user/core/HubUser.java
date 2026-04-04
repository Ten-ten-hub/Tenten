package com.team.userservice.user.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "p_hub_user")
@Getter
@NoArgsConstructor
public class HubUser extends BaseEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @JoinColumn(name = "user_id", unique = true, nullable = false)
    @OneToOne(fetch = FetchType.LAZY) // 물리 FK
    private User user;

    @Column(name = "hub_id", nullable = false) // 논리 FK // 유니크일 필요 없음
    private UUID hubId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 레코드 삭제 시간

    @Column(name = "deleted_by", length = 100)
    private UUID deletedBy; // 레코드 삭제자

    private HubUser(User user, UUID hubId) {
        this.user = user;
        this.hubId = hubId;
    }

    public static HubUser create(User user, UUID hubId) {
        return new HubUser(user, hubId);
    }

    public void updateHubId(UUID hubId) {
        this.hubId = hubId;
    }

}
