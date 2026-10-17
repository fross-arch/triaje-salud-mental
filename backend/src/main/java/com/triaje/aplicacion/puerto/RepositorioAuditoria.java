package com.triaje.aplicacion.puerto;

import com.triaje.dominio.modelo.RegistroAuditoria;

public interface RepositorioAuditoria {

    void registrar(RegistroAuditoria registro);
}
