package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    ADMIN("Admin"),
    USER("User"),
    SHIPPER("Shipper");

    private final String roleName;
}
