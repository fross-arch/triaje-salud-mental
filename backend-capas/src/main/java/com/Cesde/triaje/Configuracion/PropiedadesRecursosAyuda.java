package com.Cesde.triaje.Configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "triaje")
public class PropiedadesRecursosAyuda {

    private List<RecursoAyuda> recursosAyuda = new ArrayList<>();

    public List<RecursoAyuda> getRecursosAyuda() {
        return recursosAyuda;
    }

    public void setRecursosAyuda(List<RecursoAyuda> recursosAyuda) {
        this.recursosAyuda = recursosAyuda != null ? recursosAyuda : new ArrayList<>();
    }

    public static class RecursoAyuda {
        private String nombre;
        private String telefono;
        private String horario;
        private String descripcion;
        private String ambito;

        public RecursoAyuda() {
        }

        public RecursoAyuda(String nombre, String telefono, String horario, String descripcion, String ambito) {
            this.nombre = nombre;
            this.telefono = telefono;
            this.horario = horario;
            this.descripcion = descripcion;
            this.ambito = ambito;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getTelefono() {
            return telefono;
        }

        public void setTelefono(String telefono) {
            this.telefono = telefono;
        }

        public String getHorario() {
            return horario;
        }

        public void setHorario(String horario) {
            this.horario = horario;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getAmbito() {
            return ambito;
        }

        public void setAmbito(String ambito) {
            this.ambito = ambito;
        }
    }
}
