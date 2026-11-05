package it.exprivia.fornitori.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String cognome;
    private String telefono;
    private String targa;
    @Column(name = "targa_norm", nullable = false)
    private String targaNorm;
    @Column(name = "modello_veicolo")
    private String modelloVeicolo;
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(name = "numero_polizza")
    private String numeroPolizza;
    @Column(name = "scadenza_polizza")
    private LocalDate scadenzaPolizza;
    @Column(name = "citta_residenza")
    private String cittaResidenza;

    protected Customer() {
    }

    public Customer(
            String nome,
            String telefono,
            String targa,
            String targaNorm,
            String modelloVeicolo,
            Long companyId,
            String numeroPolizza,
            LocalDate scadenzaPolizza,
            String cittaResidenza) {
        this.nome = nome;
        this.telefono = telefono;
        this.targa = targa;
        this.targaNorm = targaNorm;
        this.modelloVeicolo = modelloVeicolo;
        this.companyId = companyId;
        this.numeroPolizza = numeroPolizza;
        this.scadenzaPolizza = scadenzaPolizza;
        this.cittaResidenza = cittaResidenza;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCognome() { return cognome; }
    public void setCognome(String value) { this.cognome = value; }
    public String getTelefono() { return telefono; }
    public String getTarga() { return targa; }
    public String getTargaNorm() { return targaNorm; }
    public String getModelloVeicolo() { return modelloVeicolo; }
    public Long getCompanyId() { return companyId; }
    public String getNumeroPolizza() { return numeroPolizza; }
    public LocalDate getScadenzaPolizza() { return scadenzaPolizza; }
    public String getCittaResidenza() { return cittaResidenza; }
}
