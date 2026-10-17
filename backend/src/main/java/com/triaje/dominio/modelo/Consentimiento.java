package com.triaje.dominio.modelo;

import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;

import java.time.Instant;
import java.util.Objects;

/**
 * Registro de consentimiento informado para la realizacion del cribado.
 */
public final class Consentimiento {

    public static final String VERSION_CONSENTIMIENTO_PREDETERMINADA = "1.0";
    public static final String VERSION_POLITICA_PREDETERMINADA = "1.0";

    private final Long id;
    private final Long usuarioId;
    private final String versionConsentimiento;
    private final String versionPolitica;
    private final boolean confirmaMayoriaEdad;
    private final Instant fechaAceptacion;
    private Instant fechaRevocacion;

    public Consentimiento(Long id, Long usuarioId, String versionConsentimiento, String versionPolitica,
                          boolean confirmaMayoriaEdad, Instant fechaAceptacion, Instant fechaRevocacion) {
        this.id = id;
        this.usuarioId = Objects.requireNonNull(usuarioId, "El identificador de usuario es obligatorio.");
        if (versionConsentimiento == null || versionConsentimiento.isBlank()) {
            throw new ExcepcionDatoInvalido("La versión del consentimiento es obligatoria.");
        }
        if (versionPolitica == null || versionPolitica.isBlank()) {
            throw new ExcepcionDatoInvalido("La versión de la política es obligatoria.");
        }
        if (!confirmaMayoriaEdad) {
            throw new ExcepcionDatoInvalido("Debes confirmar la mayoría de edad para otorgar consentimiento.");
        }
        this.versionConsentimiento = versionConsentimiento;
        this.versionPolitica = versionPolitica;
        this.confirmaMayoriaEdad = true;
        this.fechaAceptacion = Objects.requireNonNull(fechaAceptacion, "La fecha de aceptación es obligatoria.");
        this.fechaRevocacion = fechaRevocacion;
    }

    public static Consentimiento nuevo(Long usuarioId, Instant fechaAceptacion) {
        return new Consentimiento(
                null,
                usuarioId,
                VERSION_CONSENTIMIENTO_PREDETERMINADA,
                VERSION_POLITICA_PREDETERMINADA,
                true,
                fechaAceptacion,
                null
        );
    }

    public void revocar(Instant fecha) {
        this.fechaRevocacion = Objects.requireNonNull(fecha, "La fecha de revocación es obligatoria.");
    }

    public boolean estaVigente() {
        return this.fechaRevocacion == null;
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
}
