package com.team.notificationservice.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByReceiverSlackIdAndDeletedAtIsNull(String slackId, Pageable pageable);

    Page<Notification> findByReceiverSlackIdAndMsgContentContainingAndDeletedAtIsNull(
            String slackId, String msgContent, Pageable pageable);

    Optional<Notification> findByIdAndDeletedAtIsNull(UUID id);
}