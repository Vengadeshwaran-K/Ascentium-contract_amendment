package com.company.contractsystem.contract.controller;

import com.company.contractsystem.contract.dto.CreateContractRequest;
import com.company.contractsystem.contract.entity.Contract;
import com.company.contractsystem.contract.entity.ContractVersion;
import com.company.contractsystem.contract.service.ContractService;
import com.company.contractsystem.user.entity.User;
import com.company.contractsystem.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractService service;
    private final UserRepository userRepository;

    public ContractController(ContractService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @PostMapping
    public Contract create(@Valid @RequestBody CreateContractRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User creator = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return service.create(request, creator);
    }

    @PutMapping("/{id}")
    public Contract update(@PathVariable("id") Long id, @Valid @RequestBody CreateContractRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        service.submit(id, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable("id") Long id,
            @RequestParam(name = "remarks", required = false) String remarks,
            @AuthenticationPrincipal UserDetails userDetails) {
        User reviewer = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        service.approve(id, remarks, reviewer);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable("id") Long id, @RequestParam("remarks") String remarks,
            @AuthenticationPrincipal UserDetails userDetails) {
        User reviewer = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        service.reject(id, remarks, reviewer);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-contracts")
    public List<ContractVersion> getMyContracts(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return service.getMyContracts(user);
    }

    @GetMapping("/approval-queue")
    public List<ContractVersion> getApprovalQueue(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return service.getApprovalQueue(user);
    }

    @GetMapping("/all-active")
    public List<ContractVersion> getAllActiveContracts(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return service.getAllActiveContracts(user);
    }

    @GetMapping("/stats")
    public com.company.contractsystem.common.dto.DashboardStats getStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return service.getDashboardStats(user);
    }
}
