package it.exprivia.fornitori.service;

import it.exprivia.fornitori.dto.AuthDtos;
import it.exprivia.fornitori.entity.AppUser;
import it.exprivia.fornitori.entity.Driver;
import it.exprivia.fornitori.repository.AppUserRepository;
import it.exprivia.fornitori.repository.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final DriverRepository drivers;

    public AuthService(AppUserRepository users, DriverRepository drivers) {
        this.users = users;
        this.drivers = drivers;
    }

    @Transactional(readOnly = true)
    public AuthDtos.CurrentUser currentUser(String email) {
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        Driver driver = drivers.findByUserId(user.getId()).orElse(null);
        return new AuthDtos.CurrentUser(user.getId(), user.getNome(), user.getEmail(), user.getRuolo(),
                driver == null ? null : driver.getId(), driver == null ? null : driver.isDisponibile());
    }
}
