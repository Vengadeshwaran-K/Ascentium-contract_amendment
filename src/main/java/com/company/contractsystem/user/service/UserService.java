package com.company.contractsystem.user.service;

import com.company.contractsystem.user.dto.CreateUserRequest;
import com.company.contractsystem.user.entity.Role;
import com.company.contractsystem.user.entity.User;
import com.company.contractsystem.user.repository.RoleRepository;
import com.company.contractsystem.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(CreateUserRequest request) {

        if (userRepository.existsByUsername(request.username)) {
            throw new RuntimeException("Username already exists");
        }

        Role role = roleRepository.findByName(request.role)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();
        user.setUsername(request.username);
        user.setEmail(request.email);
        user.setPassword(passwordEncoder.encode(request.password));
        user.setRole(role);

        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
