import { clienteHttp } from './clienteHttp.js';

/**
 * Servicio para consultar el tablero de estadísticas agregadas y anónimas de administración.
 */
export const servicioTablero = {
  /**
   * Obtiene indicadores agregados y gráficos en base64 para un periodo en días.
   * @param {number} dias - Rango de días (7, 30, 90 o 0 para histórico completo).
   * @returns {Promise<Object>} Respuesta con indicadores y gráficos.
   */
  async obtenerTablero(dias = 30) {
    return await clienteHttp.obtener(`/api/administracion/tablero?dias=${dias}`);
  },

  /**
   * Exporta las estadísticas agregadas en formato CSV o PDF como Blob.
   * @param {number} dias - Rango de días (7, 30, 90 o 0).
   * @param {string} formato - 'csv' o 'pdf'.
   * @returns {Promise<Blob>}
   */
  async exportarTablero(dias = 30, formato = 'csv') {
    return await clienteHttp.descargarBlob(`/api/administracion/tablero/exportar?dias=${dias}&formato=${formato}`);
  }
};

