package org.joutak.loginpluginforjoutak.enums;

import lombok.Getter;

import java.util.UUID;

@Getter
public enum UUIDTypes {
    INITIAL_UUID("00000000-0000-0000-0000-000000000000");

    private final String code;
    private final UUID uuid;

    UUIDTypes(String code) {
        this.code = code;
        this.uuid = UUID.fromString(code);
    }
}
