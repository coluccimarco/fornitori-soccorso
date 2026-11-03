package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.PricingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingConfigRepository extends JpaRepository<PricingConfig, Short> {
}
