package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoRegistro;
import com.triaje.aplicacion.dto.DatosUsuario;
import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.excepcion.ExcepcionCorreoYaRegistrado;
import com.triaje.dominio.modelo.Usuario;
import com.triaje.dominio.regla.PoliticaContrasena;

import java.time.Clock;

public class RegistrarUsuario {

    private final RepositorioUsuarios repositorioUsuarios;
    private final CodificadorContrasenas codificador;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public RegistrarUsuario(RepositorioUsuarios repositorioUsuarios, CodificadorContrasenas codificador,
                            EjecutorTransaccional transacciones, Clock reloj) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.codificador = codificador;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    public DatosUsuario ejecutar(ComandoRegistro comando) {
        PoliticaContrasena.validar(comando.contrasena());
        String correo = Usuario.normalizarCorreo(comando.correo());
        // Se valida el resto de datos antes de calcular el hash (operación costosa).
        Usuario.nuevo(comando.nombreCompleto(), correo, "pendiente", reloj.instant());
        String hash = codificador.codificar(comando.contrasena());

        return transacciones.ejecutar(() -> {
            if (repositorioUsuarios.existePorCorreo(correo)) {
                throw new ExcepcionCorreoYaRegistrado();
            }
            Usuario guardado = repositorioUsuarios.guardar(
                    Usuario.nuevo(comando.nombreCompleto(), correo, hash, reloj.instant()));
            return DatosUsuario.desde(guardado);
        });
    }
}
