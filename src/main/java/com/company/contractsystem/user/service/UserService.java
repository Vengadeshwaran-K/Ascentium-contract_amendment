package com.company.contractsystem.user.service;

import com.company.contractsystem.user.dto.CreateUserRequest;
import com.company.contractsystem.user.entity.Role;
import com.company.contractsystem.user.entity.User;
import com.company.contractsystem.user.repository.RoleRepository;
import com.company.contractsystem.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
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
        user.setPassword(request.password); // auth ignored
        user.setRole(role);

        return userRepository.save(user);
    }
}
