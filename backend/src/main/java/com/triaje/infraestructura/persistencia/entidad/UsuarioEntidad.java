package com.triaje.infraestructura.persistencia.entidad;

import com.triaje.dominio.RolUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "usuarios")
public class UsuarioEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;

    @Column(name = "correo", nullable = false, unique = true, length = 120)
    private String correo;

    @Column(name = "contrasena_hash", nullable = false, length = 255)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, columnDefinition = "enum('PACIENTE','CLINICO','ADMINISTRADOR')")
    private RolUsuario rol;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "especialidad", length = 100)
    private String especialidad;

    @Column(name = "fecha_creacion", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant fechaCreacion;

    protected UsuarioEntidad() {
    }

    public UsuarioEntidad(Long id, String nombreCompleto, String correo, String contrasenaHash, RolUsuario rol,
                          boolean activo, Instant fechaCreacion, String especialidad) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
        this.especialidad = especialidad;
    }

    public UsuarioEntidad(Long id, String nombreCompleto, String correo, String contrasenaHash, RolUsuario rol,
                          boolean activo, Instant fechaCreacion) {
        this(id, nombreCompleto, correo, contrasenaHash, rol, activo, fechaCreacion, null);
    }

    public UsuarioEntidad(Long id, String nombreCompleto, String correo, String contrasenaHash, RolUsuario rol,
                          Instant fechaCreacion) {
        this(id, nombreCompleto, correo, contrasenaHash, rol, true, fechaCreacion, null);
    }

    public Long getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    @Override
    public String toString() {
        return "UsuarioEntidad{id=" + id + "}";
    }
}
