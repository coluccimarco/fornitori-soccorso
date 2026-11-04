package it.exprivia.fornitori.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class SecureConfigurationCheckTests {
    private static final String STRONG = "x".repeat(40);

    private static MockEnvironment environment(String token, boolean secureCookie) {
        return new MockEnvironment()
                .withProperty("fornitori.chat.service-token", token)
                .withProperty("spring.datasource.password", "segreto-di-prova")
                .withProperty("server.servlet.session.cookie.secure", String.valueOf(secureCookie));
    }

    private static void run(MockEnvironment environment) {
        new SecureConfigurationCheck(environment).run(new DefaultApplicationArguments());
    }

    @Test
    void weakConfigurationOnlyWarnsOutsideProd() {
        assertThatCode(() -> run(environment("corto", false))).doesNotThrowAnyException();
    }

    @Test
    void prodRefusesAShortTokenWithoutPrintingIt() {
        MockEnvironment prod = environment("token-segreto-corto", true);
        prod.setActiveProfiles("prod");
        assertThatThrownBy(() -> run(prod)).isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("token-segreto-corto");
    }

    @Test
    void prodRefusesCookiesWithoutSecureAndPlainHttpProviders() {
        MockEnvironment cookie = environment(STRONG, false);
        cookie.setActiveProfiles("prod");
        assertThatThrownBy(() -> run(cookie)).isInstanceOf(IllegalStateException.class);
        MockEnvironment provider = environment(STRONG, true).withProperty("fornitori.maps.osrm-url", "http://example.org");
        provider.setActiveProfiles("prod");
        assertThatThrownBy(() -> run(provider)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void prodAcceptsASafeConfiguration() {
        MockEnvironment prod = environment(STRONG, true);
        prod.setActiveProfiles("prod");
        assertThatCode(() -> run(prod)).doesNotThrowAnyException();
    }
}
