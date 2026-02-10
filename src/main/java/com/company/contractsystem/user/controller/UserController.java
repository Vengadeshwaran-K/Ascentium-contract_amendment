package com.company.contractsystem.user.controller;

import com.company.contractsystem.user.dto.CreateUserRequest;
import com.company.contractsystem.user.entity.User;
import com.company.contractsystem.user.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public User createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }
}
