//package com.company.contractsystem.auth.service;
//
//import com.company.contractsystem.auth.dto.LoginRequest;
//import com.company.contractsystem.auth.dto.LoginResponse;
//import com.company.contractsystem.config.JwtConfig;
//import com.company.contractsystem.user.repository.UserRepository;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//
//@Service
//public class AuthService {
//
////    private final UserRepository userRepository;
////    private final PasswordEncoder encoder;
////    private final JwtConfig jwtConfig;
////
////    public AuthService(UserRepository userRepository,
////                       PasswordEncoder encoder,
////                       JwtConfig jwtConfig) {
////        this.userRepository = userRepository;
////        this.encoder = encoder;
////        this.jwtConfig = jwtConfig;
////    }
////
////    public LoginResponse login(LoginRequest request) {
////        var user = userRepository.findByUsername(request.getUsername())
////                .orElseThrow(() -> new RuntimeException("User not found"));
////
////        if (!encoder.matches(request.getPassword(), user.getPassword())) {
////            throw new RuntimeException("Invalid credentials");
////        }
////
////        String token = jwtConfig.generateToken(user.getUsername());
////        return new LoginResponse(token);
////    }
//}
