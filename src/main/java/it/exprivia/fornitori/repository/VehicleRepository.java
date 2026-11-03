package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.Vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}
