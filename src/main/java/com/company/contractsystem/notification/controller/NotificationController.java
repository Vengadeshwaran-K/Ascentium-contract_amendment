package com.company.contractsystem.notification.controller;

import com.company.contractsystem.notification.entity.Notification;
import com.company.contractsystem.notification.repository.NotificationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/user/{userId}")
    public List<Notification> getUserNotifications(@PathVariable Long userId) {
        return repository.findAll()
                .stream()
                .filter(n -> n.getUserId().equals(userId))
                .toList();
    }
}
