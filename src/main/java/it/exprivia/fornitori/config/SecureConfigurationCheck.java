package it.exprivia.fornitori.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** All'avvio segnala una configurazione debole: blocca nel profilo prod, avvisa altrove. Non stampa mai i valori. */
@Component
class SecureConfigurationCheck implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SecureConfigurationCheck.class);
    private static final int MIN_TOKEN_LENGTH = 32;

    private final Environment environment;

    SecureConfigurationCheck(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> problems = problems();
        if (problems.isEmpty()) {
            return;
        }
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            throw new IllegalStateException("Configurazione non sicura per prod: " + String.join("; ", problems));
        }
        problems.forEach(problem -> logger.warn("Configurazione da rivedere: {}", problem));
    }

    private List<String> problems() {
        List<String> problems = new ArrayList<>();
        String token = environment.getProperty("fornitori.chat.service-token", "");
        if (token.isBlank()) {
            problems.add("token del chatbot assente, le API chat rispondono sempre 401");
        } else if (token.length() < MIN_TOKEN_LENGTH) {
            problems.add("token del chatbot più corto di " + MIN_TOKEN_LENGTH + " caratteri");
        }
        if (environment.getProperty("spring.datasource.password", "").isBlank()) {
            problems.add("password del database assente");
        }
        if (!environment.getProperty("server.servlet.session.cookie.secure", Boolean.class, false)) {
            problems.add("cookie di sessione senza Secure, ammesso solo su HTTP locale");
        }
        for (String property : List.of("fornitori.maps.nominatim-url", "fornitori.maps.osrm-url")) {
            if (environment.getProperty(property, "https://").startsWith("http://")) {
                problems.add(property + " non usa HTTPS");
            }
        }
        return problems;
    }
}
