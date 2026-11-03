package it.exprivia.fornitori.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "workshops")
public class Workshop {
    @Id
    private Long id;
    private String nome;
    private String indirizzo;
    private String citta;
    private BigDecimal lat;
    private BigDecimal lng;

    protected Workshop() {
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getIndirizzo() { return indirizzo; }
    public String getCitta() { return citta; }
    public BigDecimal getLat() { return lat; }
    public BigDecimal getLng() { return lng; }
}
