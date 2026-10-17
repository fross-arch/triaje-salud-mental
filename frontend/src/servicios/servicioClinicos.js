import { clienteHttp } from './clienteHttp.js';

export const servicioClinicos = {
  async listarActivos() {
    return await clienteHttp.obtener('/api/clinicos');
  }
};
