package com.triaje.dominio.excepcion;

public class ExcepcionCorreoYaRegistrado extends ExcepcionDominio {

    public ExcepcionCorreoYaRegistrado() {
        super("Ya existe una cuenta registrada con ese correo.");
    }
}
