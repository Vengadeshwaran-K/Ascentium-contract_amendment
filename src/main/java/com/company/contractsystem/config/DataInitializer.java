package com.company.contractsystem.config;

import com.company.contractsystem.enums.RoleType;
import com.company.contractsystem.user.entity.Role;
import com.company.contractsystem.user.entity.User;
import com.company.contractsystem.user.repository.RoleRepository;
import com.company.contractsystem.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("admin").isEmpty()) {
            Role adminRole = roleRepository.findByName(RoleType.SUPER_ADMIN)
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName(RoleType.SUPER_ADMIN);
                        return roleRepository.save(role);
                    });

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@company.com");
            admin.setRole(adminRole);
            admin.setEnabled(true);

            userRepository.save(admin);
        }
    }
}
