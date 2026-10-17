package com.triaje.infraestructura.persistencia.entidad;

import com.triaje.dominio.modelo.InstrumentoCribado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "respuestas_evaluacion",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"evaluacion_id", "instrumento", "indice_pregunta"})
        }
)
public class RespuestaEvaluacionEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "evaluacion_id", columnDefinition = "char(36)", length = 36, nullable = false)
    private String evaluacionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "instrumento", nullable = false, columnDefinition = "enum('PHQ9','GAD7')")
    private InstrumentoCribado instrumento;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "indice_pregunta", columnDefinition = "tinyint", nullable = false)
    private Integer indicePregunta;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "valor_puntaje", columnDefinition = "tinyint", nullable = false)
    private Integer valorPuntaje;

    protected RespuestaEvaluacionEntidad() {
    }

    public RespuestaEvaluacionEntidad(Long id, String evaluacionId, InstrumentoCribado instrumento,
                                      Integer indicePregunta, Integer valorPuntaje) {
        this.id = id;
        this.evaluacionId = evaluacionId;
        this.instrumento = instrumento;
        this.indicePregunta = indicePregunta;
        this.valorPuntaje = valorPuntaje;
    }

    public Long getId() {
        return id;
    }

    public String getEvaluacionId() {
        return evaluacionId;
    }

    public InstrumentoCribado getInstrumento() {
        return instrumento;
    }

    public Integer getIndicePregunta() {
        return indicePregunta;
    }

    public Integer getValorPuntaje() {
        return valorPuntaje;
    }

    public void setValorPuntaje(Integer valorPuntaje) {
        this.valorPuntaje = valorPuntaje;
    }
}
