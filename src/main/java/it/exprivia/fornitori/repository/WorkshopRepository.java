package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.Workshop;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopRepository extends JpaRepository<Workshop, Long> {
    List<Workshop> findAllByOrderByIdAsc();
}
