package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.Customer;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Query(value = """
            SELECT c.id AS id, c.nome AS nome, c.telefono AS telefono, c.targa AS targa,
                   c.modello_veicolo AS "modelloVeicolo", c.company_id AS "compagniaId",
                   co.nome AS "compagniaNome", c.numero_polizza AS "numeroPolizza",
                   c.scadenza_polizza AS "scadenzaPolizza", c.citta_residenza AS "cittaResidenza"
            FROM customers c
            JOIN insurance_companies co ON co.id = c.company_id
            WHERE lower(c.nome) LIKE concat('%', lower(:search), '%') ESCAPE '\\'
               OR (:plate <> '' AND c.targa_norm LIKE concat('%', :plate, '%'))
            ORDER BY c.id
            """, nativeQuery = true)
    List<CustomerRow> search(@Param("search") String search, @Param("plate") String normalizedPlate);

    boolean existsByTargaNorm(String targaNorm);

    Optional<Customer> findByTargaNorm(String targaNorm);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> lockById(@Param("id") Long id);

    interface CustomerRow {
        Long getId();
        String getNome();
        String getTelefono();
        String getTarga();
        String getModelloVeicolo();
        Long getCompagniaId();
        String getCompagniaNome();
        String getNumeroPolizza();
        java.time.LocalDate getScadenzaPolizza();
        String getCittaResidenza();
    }
}
