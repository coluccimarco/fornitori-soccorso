package it.exprivia.fornitori.enums;

import java.util.Locale;

public enum UserRole {
    RESPONSABILE,
    AUTISTA;

    public static UserRole fromDatabaseValue(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
