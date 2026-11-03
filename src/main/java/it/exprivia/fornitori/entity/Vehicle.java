package it.exprivia.fornitori.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    private Long id;
    private String targa;
    private String tipologia;
    @Column(name = "portata_t")
    private BigDecimal portataT;
    @Column(name = "base_indirizzo")
    private String baseIndirizzo;
    @Column(name = "base_citta")
    private String baseCitta;
    @Column(name = "base_lat")
    private BigDecimal baseLat;
    @Column(name = "base_lng")
    private BigDecimal baseLng;

    protected Vehicle() {
    }

    public Long getId() { return id; }
    public String getTarga() { return targa; }
    public String getTipologia() { return tipologia; }
    public BigDecimal getPortataT() { return portataT; }
    public String getBaseIndirizzo() { return baseIndirizzo; }
    public String getBaseCitta() { return baseCitta; }
    public BigDecimal getBaseLat() { return baseLat; }
    public BigDecimal getBaseLng() { return baseLng; }
}
