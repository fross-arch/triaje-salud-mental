package com.triaje.infraestructura.reportes;

import com.triaje.aplicacion.puerto.ClienteReportes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuracion de beans para la comunicacion con el microservicio de reportes.
 */
@Configuration
public class ConfiguracionReportes {

    @Bean
    public RestClient clienteRestReportes(RestClient.Builder constructor, PropiedadesReportes propiedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(propiedades.tiempoEsperaConexionSegundos()));
        fabrica.setReadTimeout(Duration.ofSeconds(propiedades.tiempoEsperaLecturaSegundos()));

        return constructor
                .baseUrl(propiedades.url())
                .requestFactory(fabrica)
                .build();
    }

    @Bean
    public ClienteReportes clienteReportes(RestClient clienteRestReportes, PropiedadesReportes propiedades) {
        return new AdaptadorClienteReportes(clienteRestReportes, propiedades);
    }
}
