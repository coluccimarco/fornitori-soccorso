package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.InsuranceCompany;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceCompanyRepository extends JpaRepository<InsuranceCompany, Long> {
    List<InsuranceCompany> findAllByOrderByIdAsc();
}
