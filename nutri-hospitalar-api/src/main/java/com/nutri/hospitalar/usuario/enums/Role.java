package com.nutri.hospitalar.usuario.enums;

public enum Role {
    SUPERADMIN,
    ADMIN,
    USER;

    public String authority() {
        return "ROLE_" + name();
    }
}
