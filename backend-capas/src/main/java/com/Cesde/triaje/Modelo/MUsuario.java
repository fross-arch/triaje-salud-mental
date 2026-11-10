package com.Cesde.triaje.Modelo;

import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuarios_correo", columnNames = {"correo"})
})
public class MUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;

    @Column(name = "correo", nullable = false, length = 120)
    private String correo;

    @Column(name = "contrasena_hash", nullable = false, length = 255)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private RolUsuario rol = RolUsuario.PACIENTE;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "especialidad", length = 100)
    private String especialidad;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @OneToMany(mappedBy = "usuario")
    private List<MConsentimiento> consentimientos = new ArrayList<>();

    @OneToMany(mappedBy = "usuario")
    private List<MEvaluacion> evaluaciones = new ArrayList<>();

    @OneToMany(mappedBy = "usuario")
    private List<MTokenRenovacion> tokensRenovacion = new ArrayList<>();

    @OneToMany(mappedBy = "paciente")
    private List<MAutorizacion> autorizacionesComoPaciente = new ArrayList<>();

    @OneToMany(mappedBy = "clinico")
    private List<MAutorizacion> autorizacionesComoClinico = new ArrayList<>();

    @OneToMany(mappedBy = "paciente")
    private List<MCita> citasComoPaciente = new ArrayList<>();

    @OneToMany(mappedBy = "clinico")
    private List<MCita> citasComoClinico = new ArrayList<>();

    public MUsuario() {
    }

    public MUsuario(Long id, String nombreCompleto, String correo, String contrasenaHash, RolUsuario rol,
                    boolean activo, String especialidad, Instant fechaCreacion) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.activo = activo;
        this.especialidad = especialidad;
        this.fechaCreacion = fechaCreacion;
    }

    public MUsuario(String nombreCompleto, String correo, String contrasenaHash, RolUsuario rol) {
        this(null, nombreCompleto, correo, contrasenaHash, rol, true, null, null);
    }

    public MUsuario(String nombreCompleto, String correo, String contrasenaHash, RolUsuario rol, String especialidad) {
        this(null, nombreCompleto, correo, contrasenaHash, rol, true, especialidad, null);
    }

    @PrePersist
    public void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public List<MConsentimiento> getConsentimientos() {
        return consentimientos;
    }

    public void setConsentimientos(List<MConsentimiento> consentimientos) {
        this.consentimientos = consentimientos;
    }

    public List<MEvaluacion> getEvaluaciones() {
        return evaluaciones;
    }

    public void setEvaluaciones(List<MEvaluacion> evaluaciones) {
        this.evaluaciones = evaluaciones;
    }

    public List<MTokenRenovacion> getTokensRenovacion() {
        return tokensRenovacion;
    }

    public void setTokensRenovacion(List<MTokenRenovacion> tokensRenovacion) {
        this.tokensRenovacion = tokensRenovacion;
    }

    public List<MAutorizacion> getAutorizacionesComoPaciente() {
        return autorizacionesComoPaciente;
    }

    public void setAutorizacionesComoPaciente(List<MAutorizacion> autorizacionesComoPaciente) {
        this.autorizacionesComoPaciente = autorizacionesComoPaciente;
    }

    public List<MAutorizacion> getAutorizacionesComoClinico() {
        return autorizacionesComoClinico;
    }

    public void setAutorizacionesComoClinico(List<MAutorizacion> autorizacionesComoClinico) {
        this.autorizacionesComoClinico = autorizacionesComoClinico;
    }

    public List<MCita> getCitasComoPaciente() {
        return citasComoPaciente;
    }

    public void setCitasComoPaciente(List<MCita> citasComoPaciente) {
        this.citasComoPaciente = citasComoPaciente;
    }

    public List<MCita> getCitasComoClinico() {
        return citasComoClinico;
    }

    public void setCitasComoClinico(List<MCita> citasComoClinico) {
        this.citasComoClinico = citasComoClinico;
    }
}
