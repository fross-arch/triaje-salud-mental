import { clienteHttp } from './clienteHttp.js';

export const servicioClinicoEvaluaciones = {
  async listarEvaluaciones() {
    return await clienteHttp.obtener('/api/clinico/evaluaciones');
  },

  async obtenerDetalle(evaluacionId) {
    return await clienteHttp.obtener(`/api/clinico/evaluaciones/${evaluacionId}`);
  },

  async descargarPdf(evaluacionId) {
    return await clienteHttp.descargar(`/api/clinico/evaluaciones/${evaluacionId}/pdf`);
  }
};
