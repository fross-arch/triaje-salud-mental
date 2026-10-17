package com.triaje.infraestructura.persistencia.adaptador;

import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class EjecutorTransaccionalSpring implements EjecutorTransaccional {

    private final TransactionTemplate plantilla;

    public EjecutorTransaccionalSpring(PlatformTransactionManager gestorTransacciones) {
        this.plantilla = new TransactionTemplate(gestorTransacciones);
    }

    @Override
    public <T> T ejecutar(Supplier<T> operacion) {
        return plantilla.execute(estado -> operacion.get());
    }
}
