package it.exprivia.fornitori.service;

import it.exprivia.fornitori.dto.RegistryDtos;
import it.exprivia.fornitori.entity.Customer;
import it.exprivia.fornitori.entity.InsuranceCompany;
import it.exprivia.fornitori.entity.Vehicle;
import it.exprivia.fornitori.repository.CustomerRepository;
import it.exprivia.fornitori.repository.DriverRepository;
import it.exprivia.fornitori.repository.InsuranceCompanyRepository;
import it.exprivia.fornitori.repository.VehicleRepository;
import it.exprivia.fornitori.repository.WorkshopRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistryService {
    private static final int MAX_SEARCH_LENGTH = 100;
    private static final int MIN_PLATE_LENGTH = 5;
    private static final int MAX_PLATE_LENGTH = 10;

    private final DriverRepository drivers;
    private final VehicleRepository vehicles;
    private final InsuranceCompanyRepository companies;
    private final CustomerRepository customers;
    private final WorkshopRepository workshops;

    public RegistryService(
            DriverRepository drivers,
            VehicleRepository vehicles,
            InsuranceCompanyRepository companies,
            CustomerRepository customers,
            WorkshopRepository workshops) {
        this.drivers = drivers;
        this.vehicles = vehicles;
        this.companies = companies;
        this.customers = customers;
        this.workshops = workshops;
    }

    @Transactional(readOnly = true)
    public List<RegistryDtos.DriverDto> drivers() {
        return drivers.findAllWithOperationalAvailability().stream()
                .map(row -> new RegistryDtos.DriverDto(
                        row.getId(), row.getNome(), row.getTelefono(), row.getMezzoId(),
                        row.getZona(), row.getDisponibilita()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistryDtos.VehicleDto> vehicles() {
        return vehicles.findAll().stream()
                .sorted(java.util.Comparator.comparing(Vehicle::getId))
                .map(vehicle -> new RegistryDtos.VehicleDto(
                        vehicle.getId(), vehicle.getTarga(), vehicle.getTipologia(), vehicle.getPortataT(),
                        vehicle.getBaseIndirizzo(), vehicle.getBaseCitta(), vehicle.getBaseLat(), vehicle.getBaseLng()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistryDtos.CompanyDto> companies() {
        return companies.findAllByOrderByIdAsc().stream()
                .map(company -> new RegistryDtos.CompanyDto(
                        company.getId(), company.getNome(), company.getTelefonoCentrale(), company.getEmailSinistri()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistryDtos.CustomerDto> customers(String search) {
        String term = search == null ? "" : search.trim();
        if (term.length() > MAX_SEARCH_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Testo di ricerca troppo lungo");
        }
        String normalizedPlate = term.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return customers.search(escapeLike(term), normalizedPlate).stream()
                .map(customer -> new RegistryDtos.CustomerDto(
                        customer.getId(), customer.getNome(), customer.getTelefono(), customer.getTarga(),
                        customer.getModelloVeicolo(), customer.getCompagniaId(), customer.getCompagniaNome(),
                        customer.getNumeroPolizza(), customer.getScadenzaPolizza(), customer.getCittaResidenza()))
                .toList();
    }

    @Transactional
    public RegistryDtos.CustomerDto createCustomer(RegistryDtos.CreateCustomerRequest request) {
        InsuranceCompany company = companies.findById(request.compagniaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compagnia assicurativa inesistente"));
        String normalizedPlate = request.targa().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        // Stesso formato accettato dal chatbot: una targa diversa non sarebbe mai riconosciuta.
        if (normalizedPlate.length() < MIN_PLATE_LENGTH || normalizedPlate.length() > MAX_PLATE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Targa non valida");
        }
        if (customers.existsByTargaNorm(normalizedPlate)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Targa già presente");
        }
        Customer saved = customers.save(new Customer(
                request.nome().trim(),
                request.telefono().trim(),
                request.targa().trim(),
                normalizedPlate,
                blankToNull(request.modelloVeicolo()),
                company.getId(),
                request.numeroPolizza().trim(),
                request.scadenzaPolizza(),
                blankToNull(request.cittaResidenza())));
        return new RegistryDtos.CustomerDto(
                saved.getId(), saved.getNome(), saved.getTelefono(), saved.getTarga(), saved.getModelloVeicolo(),
                company.getId(), company.getNome(), saved.getNumeroPolizza(), saved.getScadenzaPolizza(),
                saved.getCittaResidenza());
    }

    /** % e _ sono caratteri come gli altri: cercarli non deve restituire tutti i clienti. */
    private static String escapeLike(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional(readOnly = true)
    public List<RegistryDtos.WorkshopDto> workshops() {
        return workshops.findAllByOrderByIdAsc().stream()
                .map(workshop -> new RegistryDtos.WorkshopDto(
                        workshop.getId(), workshop.getNome(), workshop.getIndirizzo(), workshop.getCitta(),
                        workshop.getLat(), workshop.getLng()))
                .toList();
    }
}
