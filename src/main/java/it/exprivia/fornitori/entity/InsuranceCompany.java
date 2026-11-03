package it.exprivia.fornitori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "insurance_companies")
public class InsuranceCompany {
    @Id
    private Long id;
    private String nome;
    private String telefonoCentrale;
    private String emailSinistri;

    protected InsuranceCompany() {
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getTelefonoCentrale() { return telefonoCentrale; }
    public String getEmailSinistri() { return emailSinistri; }
}
