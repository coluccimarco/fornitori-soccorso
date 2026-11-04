package it.exprivia.fornitori.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "drivers")
public class Driver {
    @Id
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    private String nome;
    private String telefono;
    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;
    private String zona;

    protected Driver() {
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getNome() { return nome; }
    public String getTelefono() { return telefono; }
    public Long getVehicleId() { return vehicleId; }
    public String getZona() { return zona; }
}
