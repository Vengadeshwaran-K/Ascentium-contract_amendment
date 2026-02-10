package com.company.contractsystem.user.dto;

import com.company.contractsystem.enums.RoleType;

public class CreateUserRequest {

    public String username;
    public String email;
    public String password;
    public RoleType role;
}
