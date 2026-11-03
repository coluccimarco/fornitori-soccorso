package it.exprivia.fornitori.controller;

import it.exprivia.fornitori.dto.RegistryDtos;
import it.exprivia.fornitori.service.RegistryService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RegistryController {
    private final RegistryService registry;

    public RegistryController(RegistryService registry) {
        this.registry = registry;
    }

    @GetMapping("/autisti")
    public List<RegistryDtos.DriverDto> drivers() {
        return registry.drivers();
    }

    @GetMapping("/mezzi")
    public List<RegistryDtos.VehicleDto> vehicles() {
        return registry.vehicles();
    }

    @GetMapping("/compagnie")
    public List<RegistryDtos.CompanyDto> companies() {
        return registry.companies();
    }

    @GetMapping("/clienti")
    public List<RegistryDtos.CustomerDto> customers(@RequestParam(required = false) String search) {
        return registry.customers(search);
    }

    @PostMapping("/clienti")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistryDtos.CustomerDto createCustomer(
            @Valid @RequestBody RegistryDtos.CreateCustomerRequest request) {
        return registry.createCustomer(request);
    }

    @GetMapping("/officine")
    public List<RegistryDtos.WorkshopDto> workshops() {
        return registry.workshops();
    }
}
