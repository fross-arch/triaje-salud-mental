import { clienteHttp } from './clienteHttp.js';

export const servicioConsentimiento = {
  async registrarConsentimiento(confirmaMayoriaEdad = true) {
    return await clienteHttp.enviar('/api/consentimientos', {
      confirmaMayoriaEdad
    });
  },

  async consultarVigente() {
    return await clienteHttp.obtener('/api/consentimientos/vigente');
  },

  async revocar() {
    return await clienteHttp.enviar('/api/consentimientos/revocar', {});
  }
};
