package com.Cesde.triaje.Excepcion;

public class ExcepcionCorreoYaRegistrado extends ExcepcionBase {
    public ExcepcionCorreoYaRegistrado() {
        super("El correo ya se encuentra registrado.");
    }

    public ExcepcionCorreoYaRegistrado(String mensaje) {
        super(mensaje);
    }
}
