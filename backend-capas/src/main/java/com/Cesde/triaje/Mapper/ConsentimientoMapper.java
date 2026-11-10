package com.Cesde.triaje.Mapper;

import com.Cesde.triaje.Dto.RespuestaConsentimiento;
import com.Cesde.triaje.Dto.RespuestaConsentimientoVigente;
import com.Cesde.triaje.Modelo.MConsentimiento;

public final class ConsentimientoMapper {

    private ConsentimientoMapper() {
    }

    public static RespuestaConsentimiento aRespuestaConsentimiento(MConsentimiento consentimiento) {
        if (consentimiento == null) return null;
        return new RespuestaConsentimiento(
                consentimiento.getId(),
                consentimiento.getVersionConsentimiento(),
                consentimiento.getVersionPolitica(),
                consentimiento.isConfirmaMayoriaEdad(),
                consentimiento.getFechaAceptacion(),
                consentimiento.getFechaRevocacion(),
                consentimiento.estaVigente()
        );
    }

    public static RespuestaConsentimientoVigente aRespuestaConsentimientoVigente(MConsentimiento consentimiento) {
        if (consentimiento == null || !consentimiento.estaVigente()) {
            return RespuestaConsentimientoVigente.inactivo();
        }
        return RespuestaConsentimientoVigente.activo(
                consentimiento.getFechaAceptacion(),
                consentimiento.getVersionConsentimiento()
        );
    }
}
