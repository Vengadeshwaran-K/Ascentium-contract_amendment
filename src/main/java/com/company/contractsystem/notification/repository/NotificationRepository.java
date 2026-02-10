package com.company.contractsystem.notification.repository;

import com.company.contractsystem.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
