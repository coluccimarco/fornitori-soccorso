package it.exprivia.fornitori.enums;

import java.util.Arrays;
import java.util.Optional;

/** Unica fonte di verità: dal problema dichiarato in chat il backend decide il tipo di intervento. */
public enum ProblemType {
    RUOTA_CON_SCORTA("Ruota forata con ruota di scorta", "riparazione_sul_posto"),
    RUOTA_SENZA_SCORTA("Ruota forata senza ruota di scorta", "traino"),
    BATTERIA_SCARICA("Batteria scarica", "riparazione_sul_posto"),
    GUASTO_MOTORE("Guasto motore", "traino"),
    INCIDENTE("Incidente", "traino");

    private final String label;
    private final String intervention;

    ProblemType(String label, String intervention) {
        this.label = label;
        this.intervention = intervention;
    }

    public String label() {
        return label;
    }

    public String intervention() {
        return intervention;
    }

    public static Optional<ProblemType> fromCode(String code) {
        return Arrays.stream(values()).filter(type -> type.name().equals(code)).findFirst();
    }
}
