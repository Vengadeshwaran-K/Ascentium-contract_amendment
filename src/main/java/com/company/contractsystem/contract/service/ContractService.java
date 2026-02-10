package com.company.contractsystem.contract.service;

import com.company.contractsystem.contract.dto.CreateContractRequest;
import com.company.contractsystem.contract.entity.Contract;
import com.company.contractsystem.contract.entity.ContractStatus;
import com.company.contractsystem.contract.repository.ContractRepository;
import com.company.contractsystem.user.entity.User;
import com.company.contractsystem.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final UserRepository userRepository;

    public ContractService(ContractRepository contractRepository,
                           UserRepository userRepository) {
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
    }

    /**
     * CREATE CONTRACT
     * Initial state = DRAFT
     */
    public Contract create(CreateContractRequest request) {

        // 1️⃣ Validate client
        User client = userRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // 2️⃣ Create contract
        Contract contract = new Contract();
        contract.setContractName(request.getContractName());
        contract.setClient(client);
        contract.setEffectiveDate(request.getEffectiveDate());
        contract.setContractAmount(request.getContractAmount());

        // Initial status
        contract.setStatus(ContractStatus.DRAFT);
        contract.setCreatedAt(LocalDateTime.now());

        return contractRepository.save(contract);
    }

    /**
     * UPDATE CONTRACT
     * Allowed only when status = DRAFT
     */
    public Contract updateContract(Long contractId, CreateContractRequest request) {

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // ❌ Block update if not DRAFT
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT contracts can be updated");
        }

        // ✅ Update allowed fields
        contract.setContractName(request.getContractName());

        User client = userRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));
        contract.setClient(client);

        contract.setEffectiveDate(request.getEffectiveDate());
        contract.setContractAmount(request.getContractAmount());

        return contractRepository.save(contract);
    }
}
