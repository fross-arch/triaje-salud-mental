package com.triaje.infraestructura.configuracion;

import com.triaje.aplicacion.casouso.AbandonarEvaluacionesVencidas;
import com.triaje.aplicacion.casouso.ActualizarClinico;
import com.triaje.aplicacion.casouso.ActualizarPaciente;
import com.triaje.aplicacion.casouso.ActualizarPerfil;
import com.triaje.aplicacion.casouso.CambiarEstadoClinico;
import com.triaje.aplicacion.casouso.CerrarSesion;
import com.triaje.aplicacion.casouso.ConsultarConsentimientoVigente;
import com.triaje.aplicacion.casouso.CrearClinico;
import com.triaje.aplicacion.casouso.CrearEvaluacion;
import com.triaje.aplicacion.casouso.CumplirCitaClinico;
import com.triaje.aplicacion.casouso.DescargarReportePdfClinico;
import com.triaje.aplicacion.casouso.EliminarCuenta;
import com.triaje.aplicacion.casouso.EliminarClinico;
import com.triaje.aplicacion.casouso.EmisorSesion;
import com.triaje.aplicacion.casouso.FinalizarEvaluacion;
import com.triaje.aplicacion.casouso.GenerarReportePdfEvaluacion;
import com.triaje.aplicacion.casouso.IniciarSesion;
import com.triaje.aplicacion.casouso.ListarAutorizacionesEvaluacion;
import com.triaje.aplicacion.casouso.ListarClinicos;
import com.triaje.aplicacion.casouso.ListarClinicosActivos;
import com.triaje.aplicacion.casouso.ListarEvaluacionesClinico;
import com.triaje.aplicacion.casouso.ListarEvaluacionesUsuario;
import com.triaje.aplicacion.casouso.ListarPacientes;
import com.triaje.aplicacion.casouso.ObtenerDetalleEvaluacion;
import com.triaje.aplicacion.casouso.ObtenerDetalleEvaluacionClinico;
import com.triaje.aplicacion.casouso.ObtenerUsuarioActual;
import com.triaje.aplicacion.casouso.OtorgarAutorizacionAcceso;
import com.triaje.aplicacion.casouso.RegistrarConsentimiento;
import com.triaje.aplicacion.casouso.RegistrarRespuestaEvaluacion;
import com.triaje.aplicacion.casouso.RegistrarUsuario;
import com.triaje.aplicacion.casouso.RenovarSesion;
import com.triaje.aplicacion.casouso.CancelarCitaClinico;
import com.triaje.aplicacion.casouso.CancelarCitaPaciente;
import com.triaje.aplicacion.casouso.CancelarCitasPropuestasVencidas;
import com.triaje.aplicacion.casouso.ConfirmarCitaPaciente;
import com.triaje.aplicacion.casouso.ListarCitasClinico;
import com.triaje.aplicacion.casouso.ListarCitasPaciente;
import com.triaje.aplicacion.casouso.PosponerCitaClinico;
import com.triaje.aplicacion.casouso.ProponerCita;
import com.triaje.aplicacion.casouso.RechazarCitaPaciente;
import com.triaje.aplicacion.casouso.RevocarAutorizacionAcceso;
import com.triaje.aplicacion.casouso.RevocarConsentimiento;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.dominio.MaquinaEstadosCita;
import com.triaje.aplicacion.dto.DatosSolicitudReporte.DatosLineaAyudaReporte;
import com.triaje.aplicacion.puerto.ClienteReportes;
import com.triaje.aplicacion.puerto.CodificadorContrasenas;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.GeneradorTokensAcceso;
import com.triaje.aplicacion.puerto.GeneradorTokensRenovacion;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioConsentimientos;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioRespuestasEvaluacion;
import com.triaje.aplicacion.puerto.RepositorioTokensRenovacion;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.EvaluadorGad7;
import com.triaje.dominio.EvaluadorPhq9;
import com.triaje.dominio.PoliticaAccesoClinico;
import com.triaje.dominio.ReglaCrisis;
import com.triaje.dominio.ReglaResultadoMinimo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

/**
 * Ensambla los casos de uso (clases Java puras) como beans de Spring.
 */
@Configuration
public class ConfiguracionCasosUso {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    public EmisorSesion emisorSesion(GeneradorTokensAcceso generadorTokensAcceso,
                                     GeneradorTokensRenovacion generadorTokensRenovacion,
                                     RepositorioTokensRenovacion repositorioTokens, Clock reloj) {
        return new EmisorSesion(generadorTokensAcceso, generadorTokensRenovacion, repositorioTokens, reloj);
    }

    @Bean
    public RegistrarUsuario registrarUsuario(RepositorioUsuarios repositorioUsuarios,
                                             CodificadorContrasenas codificador,
                                             EjecutorTransaccional transacciones, Clock reloj) {
        return new RegistrarUsuario(repositorioUsuarios, codificador, transacciones, reloj);
    }

    @Bean
    public IniciarSesion iniciarSesion(RepositorioUsuarios repositorioUsuarios,
                                       RepositorioAuditoria repositorioAuditoria,
                                       CodificadorContrasenas codificador, EmisorSesion emisorSesion,
                                       EjecutorTransaccional transacciones, Clock reloj) {
        return new IniciarSesion(repositorioUsuarios, repositorioAuditoria, codificador, emisorSesion,
                transacciones, reloj);
    }

    @Bean
    public RenovarSesion renovarSesion(RepositorioTokensRenovacion repositorioTokens,
                                       RepositorioUsuarios repositorioUsuarios,
                                       GeneradorTokensRenovacion generadorTokensRenovacion,
                                       EmisorSesion emisorSesion, EjecutorTransaccional transacciones,
                                       Clock reloj) {
        return new RenovarSesion(repositorioTokens, repositorioUsuarios, generadorTokensRenovacion, emisorSesion,
                transacciones, reloj);
    }

    @Bean
    public CerrarSesion cerrarSesion(RepositorioTokensRenovacion repositorioTokens,
                                     GeneradorTokensRenovacion generadorTokensRenovacion,
                                     EjecutorTransaccional transacciones, Clock reloj) {
        return new CerrarSesion(repositorioTokens, generadorTokensRenovacion, transacciones, reloj);
    }

    @Bean
    public EliminarCuenta eliminarCuenta(RepositorioUsuarios repositorioUsuarios,
                                         RepositorioAuditoria repositorioAuditoria,
                                         EjecutorTransaccional transacciones, Clock reloj) {
        return new EliminarCuenta(repositorioUsuarios, repositorioAuditoria, transacciones, reloj);
    }

    @Bean
    public ObtenerUsuarioActual obtenerUsuarioActual(RepositorioUsuarios repositorioUsuarios) {
        return new ObtenerUsuarioActual(repositorioUsuarios);
    }

    @Bean
    public ActualizarPerfil actualizarPerfil(RepositorioUsuarios repositorioUsuarios,
                                             RepositorioAuditoria repositorioAuditoria,
                                             CodificadorContrasenas codificador,
                                             EjecutorTransaccional transacciones,
                                             Clock reloj) {
        return new ActualizarPerfil(repositorioUsuarios, repositorioAuditoria, codificador, transacciones, reloj);
    }

    @Bean
    public EvaluadorPhq9 evaluadorPhq9() {
        return new EvaluadorPhq9();
    }

    @Bean
    public EvaluadorGad7 evaluadorGad7() {
        return new EvaluadorGad7();
    }

    @Bean
    public ReglaCrisis reglaCrisis() {
        return new ReglaCrisis();
    }

    @Bean
    public ReglaResultadoMinimo reglaResultadoMinimo(ReglaCrisis reglaCrisis) {
        return new ReglaResultadoMinimo(reglaCrisis);
    }

    @Bean
    public PoliticaAccesoClinico politicaAccesoClinico() {
        return new PoliticaAccesoClinico();
    }

    @Bean
    public MaquinaEstadosCita maquinaEstadosCita() {
        return new MaquinaEstadosCita();
    }

    @Bean
    public RegistrarConsentimiento registrarConsentimiento(RepositorioConsentimientos repositorioConsentimientos,
                                                           RepositorioAuditoria repositorioAuditoria,
                                                           EjecutorTransaccional transacciones,
                                                           Clock reloj) {
        return new RegistrarConsentimiento(repositorioConsentimientos, repositorioAuditoria, transacciones, reloj);
    }

    @Bean
    public ConsultarConsentimientoVigente consultarConsentimientoVigente(RepositorioConsentimientos repositorioConsentimientos) {
        return new ConsultarConsentimientoVigente(repositorioConsentimientos);
    }

    @Bean
    public RevocarConsentimiento revocarConsentimiento(RepositorioConsentimientos repositorioConsentimientos,
                                                       RepositorioAutorizaciones repositorioAutorizaciones,
                                                       RepositorioCitas repositorioCitas,
                                                       RepositorioEvaluaciones repositorioEvaluaciones,
                                                       RepositorioAuditoria repositorioAuditoria,
                                                       MaquinaEstadosCita maquinaEstadosCita,
                                                       EjecutorTransaccional transacciones,
                                                       Clock reloj) {
        return new RevocarConsentimiento(repositorioConsentimientos, repositorioAutorizaciones, repositorioCitas,
                repositorioEvaluaciones, repositorioAuditoria, maquinaEstadosCita, transacciones, reloj);
    }

    @Bean
    public CrearEvaluacion crearEvaluacion(RepositorioEvaluaciones repositorioEvaluaciones,
                                           RepositorioConsentimientos repositorioConsentimientos,
                                           RepositorioCitas repositorioCitas,
                                           RepositorioAutorizaciones repositorioAutorizaciones,
                                           EjecutorTransaccional transacciones,
                                           Clock reloj) {
        return new CrearEvaluacion(repositorioEvaluaciones, repositorioConsentimientos,
                repositorioCitas, repositorioAutorizaciones, transacciones, reloj);
    }

    @Bean
    public RegistrarRespuestaEvaluacion registrarRespuestaEvaluacion(RepositorioEvaluaciones repositorioEvaluaciones,
                                                                     RepositorioRespuestasEvaluacion repositorioRespuestas,
                                                                     EjecutorTransaccional transacciones) {
        return new RegistrarRespuestaEvaluacion(repositorioEvaluaciones, repositorioRespuestas, transacciones);
    }

    @Bean
    public FinalizarEvaluacion finalizarEvaluacion(RepositorioEvaluaciones repositorioEvaluaciones,
                                                   RepositorioRespuestasEvaluacion repositorioRespuestas,
                                                   RepositorioAuditoria repositorioAuditoria,
                                                   EvaluadorPhq9 evaluadorPhq9,
                                                   EvaluadorGad7 evaluadorGad7,
                                                   ReglaCrisis reglaCrisis,
                                                   ReglaResultadoMinimo reglaResultadoMinimo,
                                                   EjecutorTransaccional transacciones,
                                                   Clock reloj) {
        return new FinalizarEvaluacion(repositorioEvaluaciones, repositorioRespuestas, repositorioAuditoria,
                evaluadorPhq9, evaluadorGad7, reglaCrisis, reglaResultadoMinimo, transacciones, reloj);
    }

    @Bean
    public ListarEvaluacionesUsuario listarEvaluacionesUsuario(RepositorioEvaluaciones repositorioEvaluaciones,
                                                               ReglaResultadoMinimo reglaResultadoMinimo) {
        return new ListarEvaluacionesUsuario(repositorioEvaluaciones, reglaResultadoMinimo);
    }

    @Bean
    public ObtenerDetalleEvaluacion obtenerDetalleEvaluacion(RepositorioEvaluaciones repositorioEvaluaciones,
                                                            RepositorioRespuestasEvaluacion repositorioRespuestas,
                                                            ReglaResultadoMinimo reglaResultadoMinimo) {
        return new ObtenerDetalleEvaluacion(repositorioEvaluaciones, repositorioRespuestas, reglaResultadoMinimo);
    }

    @Bean
    public AbandonarEvaluacionesVencidas abandonarEvaluacionesVencidas(RepositorioEvaluaciones repositorioEvaluaciones,
                                                                       EjecutorTransaccional transacciones,
                                                                       Clock reloj) {
        return new AbandonarEvaluacionesVencidas(repositorioEvaluaciones, transacciones, reloj);
    }

    @Bean
    public GenerarReportePdfEvaluacion generarReportePdfEvaluacion(RepositorioEvaluaciones repositorioEvaluaciones,
                                                                   RepositorioUsuarios repositorioUsuarios,
                                                                   RepositorioRespuestasEvaluacion repositorioRespuestas,
                                                                   RepositorioAuditoria repositorioAuditoria,
                                                                   ClienteReportes clienteReportes,
                                                                   ReglaResultadoMinimo reglaResultadoMinimo,
                                                                   PropiedadesRecursosAyuda propiedadesRecursosAyuda,
                                                                   Clock reloj) {
        List<DatosLineaAyudaReporte> lineasAyuda = propiedadesRecursosAyuda.recursosAyuda().stream()
                .map(r -> new DatosLineaAyudaReporte(r.nombre(), r.telefono(), r.horario(), r.descripcion()))
                .toList();
        return new GenerarReportePdfEvaluacion(repositorioEvaluaciones, repositorioUsuarios, repositorioRespuestas,
                repositorioAuditoria, clienteReportes, reglaResultadoMinimo, lineasAyuda, reloj);
    }

    @Bean
    public CrearClinico crearClinico(RepositorioUsuarios repositorioUsuarios,
                                     RepositorioAuditoria repositorioAuditoria,
                                     CodificadorContrasenas codificador,
                                     EjecutorTransaccional transacciones,
                                     Clock reloj) {
        return new CrearClinico(repositorioUsuarios, repositorioAuditoria, codificador, transacciones, reloj);
    }

    @Bean
    public ListarClinicos listarClinicos(RepositorioUsuarios repositorioUsuarios) {
        return new ListarClinicos(repositorioUsuarios);
    }

    @Bean
    public CambiarEstadoClinico cambiarEstadoClinico(RepositorioUsuarios repositorioUsuarios,
                                                     RepositorioAuditoria repositorioAuditoria,
                                                     RepositorioTokensRenovacion repositorioTokens,
                                                     EjecutorTransaccional transacciones,
                                                     Clock reloj) {
        return new CambiarEstadoClinico(repositorioUsuarios, repositorioAuditoria, repositorioTokens, transacciones, reloj);
    }

    @Bean
    public ListarClinicosActivos listarClinicosActivos(RepositorioUsuarios repositorioUsuarios) {
        return new ListarClinicosActivos(repositorioUsuarios);
    }

    @Bean
    public ActualizarClinico actualizarClinico(RepositorioUsuarios repositorioUsuarios,
                                             RepositorioAuditoria repositorioAuditoria,
                                             CodificadorContrasenas codificador,
                                             EjecutorTransaccional transacciones,
                                             Clock reloj) {
        return new ActualizarClinico(repositorioUsuarios, repositorioAuditoria, codificador, transacciones, reloj);
    }

    @Bean
    public ActualizarPaciente actualizarPaciente(RepositorioUsuarios repositorioUsuarios,
                                               RepositorioAuditoria repositorioAuditoria,
                                               CodificadorContrasenas codificador,
                                               EjecutorTransaccional transacciones,
                                               Clock reloj) {
        return new ActualizarPaciente(repositorioUsuarios, repositorioAuditoria, codificador, transacciones, reloj);
    }

    @Bean
    public EliminarClinico eliminarClinico(RepositorioUsuarios repositorioUsuarios,
                                           RepositorioAuditoria repositorioAuditoria,
                                           EjecutorTransaccional transacciones,
                                           Clock reloj) {
        return new EliminarClinico(repositorioUsuarios, repositorioAuditoria, transacciones, reloj);
    }

    @Bean
    public ListarPacientes listarPacientes(RepositorioUsuarios repositorioUsuarios) {
        return new ListarPacientes(repositorioUsuarios);
    }

    @Bean
    public OtorgarAutorizacionAcceso otorgarAutorizacionAcceso(RepositorioAutorizaciones repositorioAutorizaciones,
                                                             RepositorioEvaluaciones repositorioEvaluaciones,
                                                             RepositorioConsentimientos repositorioConsentimientos,
                                                             RepositorioUsuarios repositorioUsuarios,
                                                             RepositorioAuditoria repositorioAuditoria,
                                                             EjecutorTransaccional transacciones,
                                                             Clock reloj) {
        return new OtorgarAutorizacionAcceso(repositorioAutorizaciones, repositorioEvaluaciones,
                repositorioConsentimientos, repositorioUsuarios, repositorioAuditoria, transacciones, reloj);
    }

    @Bean
    public ListarAutorizacionesEvaluacion listarAutorizacionesEvaluacion(RepositorioAutorizaciones repositorioAutorizaciones,
                                                                       RepositorioEvaluaciones repositorioEvaluaciones) {
        return new ListarAutorizacionesEvaluacion(repositorioAutorizaciones, repositorioEvaluaciones);
    }

    @Bean
    public RevocarAutorizacionAcceso revocarAutorizacionAcceso(RepositorioAutorizaciones repositorioAutorizaciones,
                                                             RepositorioEvaluaciones repositorioEvaluaciones,
                                                             RepositorioCitas repositorioCitas,
                                                             RepositorioAuditoria repositorioAuditoria,
                                                             MaquinaEstadosCita maquinaEstadosCita,
                                                             EjecutorTransaccional transacciones,
                                                             Clock reloj) {
        return new RevocarAutorizacionAcceso(repositorioAutorizaciones, repositorioEvaluaciones, repositorioCitas,
                repositorioAuditoria, maquinaEstadosCita, transacciones, reloj);
    }

    @Bean
    public ProponerCita proponerCita(RepositorioCitas repositorioCitas,
                                     RepositorioAutorizaciones repositorioAutorizaciones,
                                     RepositorioUsuarios repositorioUsuarios,
                                     RepositorioAuditoria repositorioAuditoria,
                                     MaquinaEstadosCita maquinaEstadosCita,
                                     EjecutorTransaccional transacciones,
                                     Clock reloj) {
        return new ProponerCita(repositorioCitas, repositorioAutorizaciones, repositorioUsuarios,
                repositorioAuditoria, maquinaEstadosCita, transacciones, reloj);
    }

    @Bean
    public ListarCitasClinico listarCitasClinico(RepositorioCitas repositorioCitas,
                                                 RepositorioUsuarios repositorioUsuarios) {
        return new ListarCitasClinico(repositorioCitas, repositorioUsuarios);
    }

    @Bean
    public CancelarCitaClinico cancelarCitaClinico(RepositorioCitas repositorioCitas,
                                                   RepositorioUsuarios repositorioUsuarios,
                                                   RepositorioAuditoria repositorioAuditoria,
                                                   MaquinaEstadosCita maquinaEstadosCita,
                                                   EjecutorTransaccional transacciones,
                                                   Clock reloj) {
        return new CancelarCitaClinico(repositorioCitas, repositorioUsuarios, repositorioAuditoria,
                maquinaEstadosCita, transacciones, reloj);
    }

    @Bean
    public CumplirCitaClinico cumplirCitaClinico(RepositorioCitas repositorioCitas,
                                                 RepositorioUsuarios repositorioUsuarios,
                                                 RepositorioAuditoria repositorioAuditoria,
                                                 MaquinaEstadosCita maquinaEstadosCita,
                                                 EjecutorTransaccional transacciones,
                                                 Clock reloj) {
        return new CumplirCitaClinico(repositorioCitas, repositorioUsuarios, repositorioAuditoria,
                maquinaEstadosCita, transacciones, reloj);
    }

    @Bean
    public PosponerCitaClinico posponerCitaClinico(RepositorioCitas repositorioCitas,
                                                   RepositorioUsuarios repositorioUsuarios,
                                                   RepositorioAuditoria repositorioAuditoria,
                                                   MaquinaEstadosCita maquinaEstadosCita,
                                                   EjecutorTransaccional transacciones,
                                                   Clock reloj) {
        return new PosponerCitaClinico(repositorioCitas, repositorioUsuarios, repositorioAuditoria,
                maquinaEstadosCita, transacciones, reloj);
    }

    @Bean
    public ListarCitasPaciente listarCitasPaciente(RepositorioCitas repositorioCitas,
                                                   RepositorioUsuarios repositorioUsuarios) {
        return new ListarCitasPaciente(repositorioCitas, repositorioUsuarios);
    }

    @Bean
    public ConfirmarCitaPaciente confirmarCitaPaciente(RepositorioCitas repositorioCitas,
                                                       RepositorioUsuarios repositorioUsuarios,
                                                       RepositorioAuditoria repositorioAuditoria,
                                                       MaquinaEstadosCita maquinaEstadosCita,
                                                       EjecutorTransaccional transacciones,
                                                       Clock reloj) {
        return new ConfirmarCitaPaciente(repositorioCitas, repositorioUsuarios, repositorioAuditoria,
                maquinaEstadosCita, transacciones, reloj);
    }

    @Bean
    public RechazarCitaPaciente rechazarCitaPaciente(RepositorioCitas repositorioCitas,
                                                     RepositorioUsuarios repositorioUsuarios,
                                                     RepositorioAuditoria repositorioAuditoria,
                                                     MaquinaEstadosCita maquinaEstadosCita,
                                                     EjecutorTransaccional transacciones,
                                                     Clock reloj) {
        return new RechazarCitaPaciente(repositorioCitas, repositorioUsuarios, repositorioAuditoria,
                maquinaEstadosCita, transacciones, reloj);
    }

    @Bean
    public CancelarCitaPaciente cancelarCitaPaciente(RepositorioCitas repositorioCitas,
                                                     RepositorioUsuarios repositorioUsuarios,
                                                     RepositorioAuditoria repositorioAuditoria,
                                                     MaquinaEstadosCita maquinaEstadosCita,
                                                     EjecutorTransaccional transacciones,
                                                     Clock reloj) {
        return new CancelarCitaPaciente(repositorioCitas, repositorioUsuarios, repositorioAuditoria,
                maquinaEstadosCita, transacciones, reloj);
    }

    @Bean
    public CancelarCitasPropuestasVencidas cancelarCitasPropuestasVencidas(RepositorioCitas repositorioCitas,
                                                                           RepositorioAuditoria repositorioAuditoria,
                                                                           MaquinaEstadosCita maquinaEstadosCita,
                                                                           EjecutorTransaccional transacciones,
                                                                           Clock reloj) {
        return new CancelarCitasPropuestasVencidas(repositorioCitas, repositorioAuditoria, maquinaEstadosCita, transacciones, reloj);
    }

    @Bean
    public ListarEvaluacionesClinico listarEvaluacionesClinico(RepositorioAutorizaciones repositorioAutorizaciones,
                                                             RepositorioEvaluaciones repositorioEvaluaciones,
                                                             RepositorioUsuarios repositorioUsuarios,
                                                             RepositorioAuditoria repositorioAuditoria,
                                                             ReglaResultadoMinimo reglaResultadoMinimo,
                                                             EjecutorTransaccional transacciones,
                                                             Clock reloj) {
        return new ListarEvaluacionesClinico(repositorioAutorizaciones, repositorioEvaluaciones,
                repositorioUsuarios, repositorioAuditoria, reglaResultadoMinimo, transacciones, reloj);
    }

    @Bean
    public ObtenerDetalleEvaluacionClinico obtenerDetalleEvaluacionClinico(RepositorioAutorizaciones repositorioAutorizaciones,
                                                                         RepositorioEvaluaciones repositorioEvaluaciones,
                                                                         RepositorioUsuarios repositorioUsuarios,
                                                                         RepositorioRespuestasEvaluacion repositorioRespuestas,
                                                                         RepositorioAuditoria repositorioAuditoria,
                                                                         PoliticaAccesoClinico politicaAccesoClinico,
                                                                         ReglaResultadoMinimo reglaResultadoMinimo,
                                                                         EjecutorTransaccional transacciones,
                                                                         Clock reloj) {
        return new ObtenerDetalleEvaluacionClinico(repositorioAutorizaciones, repositorioEvaluaciones,
                repositorioUsuarios, repositorioRespuestas, repositorioAuditoria, politicaAccesoClinico,
                reglaResultadoMinimo, transacciones, reloj);
    }

    @Bean
    public DescargarReportePdfClinico descargarReportePdfClinico(RepositorioAutorizaciones repositorioAutorizaciones,
                                                               RepositorioEvaluaciones repositorioEvaluaciones,
                                                               RepositorioUsuarios repositorioUsuarios,
                                                               RepositorioRespuestasEvaluacion repositorioRespuestas,
                                                               RepositorioAuditoria repositorioAuditoria,
                                                               ClienteReportes clienteReportes,
                                                               PoliticaAccesoClinico politicaAccesoClinico,
                                                               ReglaResultadoMinimo reglaResultadoMinimo,
                                                               PropiedadesRecursosAyuda propiedadesRecursosAyuda,
                                                               EjecutorTransaccional transacciones,
                                                               Clock reloj) {
        List<DatosLineaAyudaReporte> lineasAyuda = propiedadesRecursosAyuda.recursosAyuda().stream()
                .map(r -> new DatosLineaAyudaReporte(r.nombre(), r.telefono(), r.horario(), r.descripcion()))
                .toList();
        return new DescargarReportePdfClinico(repositorioAutorizaciones, repositorioEvaluaciones,
                repositorioUsuarios, repositorioRespuestas, repositorioAuditoria, clienteReportes,
                politicaAccesoClinico, reglaResultadoMinimo, lineasAyuda, transacciones, reloj);
    }
}
