package com.Cesde.triaje.Modelo;

import com.Cesde.triaje.Modelo.Enumeracion.InstrumentoCribado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "respuestas_evaluacion", uniqueConstraints = {
        @UniqueConstraint(name = "uk_respuestas_evaluacion_item", columnNames = {"evaluacion_id", "instrumento", "indice_pregunta"})
})
public class MRespuestaEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private MEvaluacion evaluacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "instrumento", nullable = false, length = 10)
    private InstrumentoCribado instrumento;

    @Column(name = "indice_pregunta", nullable = false)
    private int indicePregunta;

    @Column(name = "valor_puntaje", nullable = false)
    private int valorPuntaje;

    public MRespuestaEvaluacion() {
    }

    public MRespuestaEvaluacion(Long id, MEvaluacion evaluacion, InstrumentoCribado instrumento, int indicePregunta, int valorPuntaje) {
        this.id = id;
        this.evaluacion = evaluacion;
        this.instrumento = instrumento;
        this.indicePregunta = indicePregunta;
        this.valorPuntaje = valorPuntaje;
    }

    public MRespuestaEvaluacion(MEvaluacion evaluacion, InstrumentoCribado instrumento, int indicePregunta, int valorPuntaje) {
        this(null, evaluacion, instrumento, indicePregunta, valorPuntaje);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MEvaluacion getEvaluacion() {
        return evaluacion;
    }

    public void setEvaluacion(MEvaluacion evaluacion) {
        this.evaluacion = evaluacion;
    }

    public InstrumentoCribado getInstrumento() {
        return instrumento;
    }

    public void setInstrumento(InstrumentoCribado instrumento) {
        this.instrumento = instrumento;
    }

    public int getIndicePregunta() {
        return indicePregunta;
    }

    public void setIndicePregunta(int indicePregunta) {
        this.indicePregunta = indicePregunta;
    }

    public int getValorPuntaje() {
        return valorPuntaje;
    }

    public void setValorPuntaje(int valorPuntaje) {
        this.valorPuntaje = valorPuntaje;
    }
}
