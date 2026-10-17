import { clienteHttp } from './clienteHttp.js';

export const servicioTriaje = {
  async crearEvaluacion() {
    return await clienteHttp.enviar('/api/triaje/evaluaciones', {});
  },

  async registrarRespuesta(evaluacionId, instrumento, indicePregunta, valorPuntaje) {
    return await clienteHttp.actualizar(`/api/triaje/evaluaciones/${evaluacionId}/respuestas`, {
      instrumento,
      indicePregunta,
      valorPuntaje
    });
  },

  async finalizarEvaluacion(evaluacionId) {
    return await clienteHttp.enviar(`/api/triaje/evaluaciones/${evaluacionId}/finalizar`, {});
  },

  async listarEvaluaciones() {
    return await clienteHttp.obtener('/api/triaje/evaluaciones');
  },

  async obtenerDetalle(evaluacionId) {
    return await clienteHttp.obtener(`/api/triaje/evaluaciones/${evaluacionId}`);
  },

  async descargarPdf(evaluacionId) {
    return await clienteHttp.descargar(`/api/triaje/evaluaciones/${evaluacionId}/pdf`);
  },

  async otorgarAutorizacion(evaluacionId, clinicoId, confirmaAutorizacion = true) {
    return await clienteHttp.enviar(`/api/triaje/evaluaciones/${evaluacionId}/autorizaciones`, {
      clinicoId,
      confirmaAutorizacion
    });
  },

  async listarAutorizaciones(evaluacionId) {
    return await clienteHttp.obtener(`/api/triaje/evaluaciones/${evaluacionId}/autorizaciones`);
  },

  async revocarAutorizacion(evaluacionId, clinicoId) {
    return await clienteHttp.enviar(`/api/triaje/evaluaciones/${evaluacionId}/autorizaciones/${clinicoId}/revocar`, {});
  }
};
