package com.triaje.dominio.modelo;

import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Usuario del sistema. Se aplica minimización de datos: solo nombre, correo y credenciales.
 */
public final class Usuario {

    public static final int LONGITUD_MAXIMA_NOMBRE = 120;
    public static final int LONGITUD_MAXIMA_CORREO = 120;

    private final Long id;
    private final String nombreCompleto;
    private final String correo;
    private final String contrasenaHash;
    private final RolUsuario rol;
    private final boolean activo;
    private final Instant fechaCreacion;
    private final String especialidad;

    public Usuario(Long id, String nombreCompleto, String correo, String contrasenaHash, RolUsuario rol,
                   boolean activo, Instant fechaCreacion, String especialidad) {
        this.id = id;
        this.nombreCompleto = validarNombre(nombreCompleto);
        this.correo = normalizarCorreo(correo);
        if (contrasenaHash == null || contrasenaHash.isBlank()) {
            throw new ExcepcionDatoInvalido("El hash de la contraseña es obligatorio.");
        }
        this.contrasenaHash = contrasenaHash;
        this.rol = Objects.requireNonNull(rol, "El rol es obligatorio");
        this.activo = activo;
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "La fecha de creación es obligatoria");
        if (especialidad != null && especialidad.length() > 100) {
            throw new ExcepcionDatoInvalido("La especialidad no puede superar 100 caracteres.");
        }
        this.especialidad = (especialidad != null && !especialidad.isBlank()) ? especialidad.trim() : null;
    }

    public Usuario(Long id, String nombreCompleto, String correo, String contrasenaHash, RolUsuario rol,
                   boolean activo, Instant fechaCreacion) {
        this(id, nombreCompleto, correo, contrasenaHash, rol, activo, fechaCreacion, null);
    }

    public Usuario(Long id, String nombreCompleto, String correo, String contrasenaHash, RolUsuario rol,
                   Instant fechaCreacion) {
        this(id, nombreCompleto, correo, contrasenaHash, rol, true, fechaCreacion, null);
    }

    public Usuario(Long id, String nombreCompleto, String correo, String contrasenaHash, Rol rol,
                   Instant fechaCreacion) {
        this(id, nombreCompleto, correo, contrasenaHash, RolUsuario.valueOf(rol.name()), true, fechaCreacion, null);
    }

    /**
     * Crea un usuario nuevo (aún sin identificador) con rol PACIENTE y activo=true.
     */
    public static Usuario nuevo(String nombreCompleto, String correo, String contrasenaHash, Instant fechaCreacion) {
        return new Usuario(null, nombreCompleto, correo, contrasenaHash, RolUsuario.PACIENTE, true, fechaCreacion, null);
    }

    /**
     * Crea un usuario clínico nuevo con rol CLINICO, especialidad y activo=true.
     */
    public static Usuario nuevoClinico(String nombreCompleto, String correo, String contrasenaHash, String especialidad, Instant fechaCreacion) {
        return new Usuario(null, nombreCompleto, correo, contrasenaHash, RolUsuario.CLINICO, true, fechaCreacion, especialidad);
    }

    /**
     * Crea un usuario clínico nuevo con rol CLINICO y activo=true sin especialidad.
     */
    public static Usuario nuevoClinico(String nombreCompleto, String correo, String contrasenaHash, Instant fechaCreacion) {
        return nuevoClinico(nombreCompleto, correo, contrasenaHash, null, fechaCreacion);
    }

    /**
     * Crea un usuario administrador nuevo con rol ADMINISTRADOR y activo=true.
     */
    public static Usuario nuevoAdministrador(String nombreCompleto, String correo, String contrasenaHash, Instant fechaCreacion) {
        return new Usuario(null, nombreCompleto, correo, contrasenaHash, RolUsuario.ADMINISTRADOR, true, fechaCreacion);
    }

    /**
     * Normaliza un correo para compararlo y guardarlo de forma consistente.
     */
    public static String normalizarCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ExcepcionDatoInvalido("El correo es obligatorio.");
        }
        String normalizado = correo.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() > LONGITUD_MAXIMA_CORREO) {
            throw new ExcepcionDatoInvalido("El correo no puede superar " + LONGITUD_MAXIMA_CORREO + " caracteres.");
        }
        return normalizado;
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ExcepcionDatoInvalido("El nombre completo es obligatorio.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new ExcepcionDatoInvalido("El nombre completo no puede superar " + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
        }
        return limpio;
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

    public Usuario conActivo(boolean nuevoActivo) {
        return new Usuario(this.id, this.nombreCompleto, this.correo, this.contrasenaHash, this.rol, nuevoActivo, this.fechaCreacion, this.especialidad);
    }

    public Usuario conDatosClinico(String nuevoNombre, String nuevoCorreo, String nuevaEspecialidad) {
        return conDatosClinico(nuevoNombre, nuevoCorreo, nuevaEspecialidad, this.contrasenaHash);
    }

    public Usuario conDatosClinico(String nuevoNombre, String nuevoCorreo, String nuevaEspecialidad, String nuevoHash) {
        return new Usuario(this.id, nuevoNombre, nuevoCorreo, nuevoHash != null ? nuevoHash : this.contrasenaHash, this.rol, this.activo, this.fechaCreacion, nuevaEspecialidad);
    }

    public Usuario conDatosPaciente(String nuevoNombre, String nuevoCorreo, String nuevoHash) {
        return new Usuario(this.id, nuevoNombre, nuevoCorreo, nuevoHash != null ? nuevoHash : this.contrasenaHash, this.rol, this.activo, this.fechaCreacion, this.especialidad);
    }

    public Usuario conPerfil(String nuevoNombre, String nuevoCorreo, String nuevaEspecialidad, String nuevoHash) {
        String especialidadEfectiva = (this.rol == RolUsuario.CLINICO) ? nuevaEspecialidad : this.especialidad;
        return new Usuario(this.id, nuevoNombre, nuevoCorreo, nuevoHash != null ? nuevoHash : this.contrasenaHash, this.rol, this.activo, this.fechaCreacion, especialidadEfectiva);
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    /**
     * Representación sin datos personales, segura para trazas.
     */
    @Override
    public String toString() {
        return "Usuario{id=" + id + ", rol=" + rol + ", activo=" + activo + "}";
    }
}
