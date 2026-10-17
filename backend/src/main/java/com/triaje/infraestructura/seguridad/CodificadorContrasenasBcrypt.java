package com.triaje.infraestructura.seguridad;

import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CodificadorContrasenasBcrypt implements CodificadorContrasenas {

    private final PasswordEncoder codificador;

    public CodificadorContrasenasBcrypt(PasswordEncoder codificador) {
        this.codificador = codificador;
    }

    @Override
    public String codificar(String contrasenaPlana) {
        return codificador.encode(contrasenaPlana);
    }

    @Override
    public boolean coincide(String contrasenaPlana, String contrasenaHash) {
        return codificador.matches(contrasenaPlana, contrasenaHash);
    }
}
