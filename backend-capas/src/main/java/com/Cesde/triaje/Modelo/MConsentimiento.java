package com.Cesde.triaje.Modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "consentimientos")
public class MConsentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private MUsuario usuario;

    @Column(name = "version_consentimiento", nullable = false, length = 20)
    private String versionConsentimiento = "1.0";

    @Column(name = "version_politica", nullable = false, length = 20)
    private String versionPolitica = "1.0";

    @Column(name = "confirma_mayoria_edad", nullable = false)
    private boolean confirmaMayoriaEdad;

    @Column(name = "fecha_aceptacion", nullable = false, updatable = false)
    private Instant fechaAceptacion;

    @Column(name = "fecha_revocacion")
    private Instant fechaRevocacion;

    public MConsentimiento() {
    }

    public MConsentimiento(Long id, MUsuario usuario, String versionConsentimiento, String versionPolitica,
                           boolean confirmaMayoriaEdad, Instant fechaAceptacion, Instant fechaRevocacion) {
        this.id = id;
        this.usuario = usuario;
        this.versionConsentimiento = versionConsentimiento;
        this.versionPolitica = versionPolitica;
        this.confirmaMayoriaEdad = confirmaMayoriaEdad;
        this.fechaAceptacion = fechaAceptacion;
        this.fechaRevocacion = fechaRevocacion;
    }

    public MConsentimiento(MUsuario usuario, boolean confirmaMayoriaEdad) {
        this(null, usuario, "1.0", "1.0", confirmaMayoriaEdad, null, null);
    }

    @PrePersist
    public void alCrear() {
        if (fechaAceptacion == null) {
            fechaAceptacion = Instant.now();
        }
    }

    public boolean estaVigente() {
        return this.fechaRevocacion == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MUsuario getUsuario() {
        return usuario;
    }

    public void setUsuario(MUsuario usuario) {
        this.usuario = usuario;
    }

    public String getVersionConsentimiento() {
        return versionConsentimiento;
    }

    public void setVersionConsentimiento(String versionConsentimiento) {
        this.versionConsentimiento = versionConsentimiento;
    }

    public String getVersionPolitica() {
        return versionPolitica;
    }

    public void setVersionPolitica(String versionPolitica) {
        this.versionPolitica = versionPolitica;
    }

    public boolean isConfirmaMayoriaEdad() {
        return confirmaMayoriaEdad;
    }

    public void setConfirmaMayoriaEdad(boolean confirmaMayoriaEdad) {
        this.confirmaMayoriaEdad = confirmaMayoriaEdad;
    }

    public Instant getFechaAceptacion() {
        return fechaAceptacion;
    }

    public void setFechaAceptacion(Instant fechaAceptacion) {
        this.fechaAceptacion = fechaAceptacion;
    }

    public Instant getFechaRevocacion() {
        return fechaRevocacion;
    }

    public void setFechaRevocacion(Instant fechaRevocacion) {
        this.fechaRevocacion = fechaRevocacion;
    }
}
