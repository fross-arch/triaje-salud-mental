package com.triaje.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "consentimientos")
public class ConsentimientoEntidad {

    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "version_consentimiento", nullable = false, length = 20)
    private String versionConsentimiento;

    @Column(name = "version_politica", nullable = false, length = 20)
    private String versionPolitica;

    @Column(name = "confirma_mayoria_edad", nullable = false)
    private boolean confirmaMayoriaEdad;

    @Column(name = "fecha_aceptacion", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant fechaAceptacion;

    @Column(name = "fecha_revocacion", columnDefinition = "TIMESTAMP")
    private Instant fechaRevocacion;

    protected ConsentimientoEntidad() {
    }

    public ConsentimientoEntidad(Long id, Long usuarioId, String versionConsentimiento,
                                String versionPolitica, boolean confirmaMayoriaEdad,
                                Instant fechaAceptacion, Instant fechaRevocacion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.versionConsentimiento = versionConsentimiento;
        this.versionPolitica = versionPolitica;
        this.confirmaMayoriaEdad = confirmaMayoriaEdad;
        this.fechaAceptacion = fechaAceptacion;
        this.fechaRevocacion = fechaRevocacion;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getVersionConsentimiento() {
        return versionConsentimiento;
    }

    public String getVersionPolitica() {
        return versionPolitica;
    }

    public boolean isConfirmaMayoriaEdad() {
        return confirmaMayoriaEdad;
    }

    public Instant getFechaAceptacion() {
        return fechaAceptacion;
    }

    public Instant getFechaRevocacion() {
        return fechaRevocacion;
    }

    public void setFechaRevocacion(Instant fechaRevocacion) {
        this.fechaRevocacion = fechaRevocacion;
    }
}
