package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.Driver;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    @Query(value = """
            SELECT d.id AS id, d.nome AS nome, d.telefono AS telefono,
                   d.vehicle_id AS "mezzoId", d.zona AS zona,
                   CASE WHEN EXISTS (
                       SELECT 1 FROM missions m
                       WHERE m.driver_id = d.id AND m.is_fixture = FALSE
                         AND m.stato IN ('proposta', 'accettata')
                   ) THEN 'occupato' ELSE 'disponibile' END AS "disponibilita"
            FROM drivers d
            ORDER BY d.id
            """, nativeQuery = true)
    List<DriverAvailabilityRow> findAllWithOperationalAvailability();

    Optional<Driver> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Driver d where d.id = :id")
    Optional<Driver> lockById(@Param("id") Long id);

    @Query("select d.id from Driver d where d.userId = :userId")
    Optional<Long> findIdByUserId(@Param("userId") Long userId);

    interface DriverAvailabilityRow {
        Long getId();
        String getNome();
        String getTelefono();
        Long getMezzoId();
        String getZona();
        String getDisponibilita();
    }
}
