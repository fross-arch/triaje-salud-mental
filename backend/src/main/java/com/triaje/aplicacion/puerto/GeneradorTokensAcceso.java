package com.triaje.aplicacion.puerto;

import com.triaje.dominio.modelo.Usuario;

public interface GeneradorTokensAcceso {

    TokenAcceso generar(Usuario usuario);

    /**
     * @param valor             token firmado
     * @param expiraEnSegundos  segundos de vigencia desde su emisión
     */
    record TokenAcceso(String valor, long expiraEnSegundos) {
    }
}
