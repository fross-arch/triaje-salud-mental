package com.triaje.infraestructura.reportes;

import com.triaje.aplicacion.dto.DatosSolicitudReporte;
import com.triaje.aplicacion.puerto.ClienteReportes;
import com.triaje.dominio.excepcion.ExcepcionServicioReportesNoDisponible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Objects;

/**
 * Adaptador de infraestructura que se comunica mediante RestClient
 * con el microservicio interno generador de reportes PDF.
 */
public class AdaptadorClienteReportes implements ClienteReportes {

    private static final Logger REGISTRO = LoggerFactory.getLogger(AdaptadorClienteReportes.class);
    private static final String CABECERA_CLAVE_INTERNA = "X-Clave-Interna";

    private final RestClient clienteRest;
    private final PropiedadesReportes propiedades;

    public AdaptadorClienteReportes(RestClient clienteRest, PropiedadesReportes propiedades) {
        this.clienteRest = Objects.requireNonNull(clienteRest, "El cliente REST es obligatorio");
        this.propiedades = Objects.requireNonNull(propiedades, "Las propiedades de reportes son obligatorias");
    }

    @Override
    public byte[] generarReportePdf(DatosSolicitudReporte datos) {
        PeticionReporteDto peticion = PeticionReporteDto.desde(datos);
        try {
            byte[] resultado = clienteRest.post()
                    .uri("/api/reportes/generar")
                    .header(CABECERA_CLAVE_INTERNA, propiedades.claveInterna())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_PDF)
                    .body(peticion)
                    .retrieve()
                    .body(byte[].class);

            if (resultado == null || resultado.length == 0) {
                REGISTRO.error("El microservicio de reportes devolvio un cuerpo vacio");
                throw new ExcepcionServicioReportesNoDisponible(
                        "El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde."
                );
            }
            return resultado;
        } catch (RestClientException e) {
            // Regla de privacidad: cero datos de pacientes o respuestas en los logs de error
            REGISTRO.error("Error al comunicar con el microservicio de reportes ({})", e.getClass().getSimpleName());
            throw new ExcepcionServicioReportesNoDisponible(
                    "El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde.",
                    e
            );
        }
    }
}
