import { clienteHttp } from './clienteHttp.js';

export const servicioRecursosAyuda = {
  async obtenerRecursos() {
    return await clienteHttp.obtener('/api/recursos-ayuda');
  }
};
