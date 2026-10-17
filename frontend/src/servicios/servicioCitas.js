import { clienteHttp } from './clienteHttp.js';

export const servicioCitas = {
  // Endpoints para PACIENTE
  async listarCitasPaciente() {
    return await clienteHttp.obtener('/api/citas');
  },

  async confirmarCita(id) {
    return await clienteHttp.enviar(`/api/citas/${id}/confirmar`, {});
  },

  async rechazarCita(id) {
    return await clienteHttp.enviar(`/api/citas/${id}/rechazar`, {});
  },

  async cancelarCitaPaciente(id) {
    return await clienteHttp.enviar(`/api/citas/${id}/cancelar`, {});
  },

  // Endpoints para CLINICO
  async listarCitasClinico() {
    return await clienteHttp.obtener('/api/clinico/citas');
  },

  async proponerCita(datos) {
    return await clienteHttp.enviar('/api/clinico/citas', {
      evaluacionId: datos.evaluacionId,
      fechaHora: datos.fechaHora,
      modalidad: datos.modalidad,
      mensaje: datos.mensaje
    });
  },

  async posponerCita(id, datos) {
    return await clienteHttp.actualizar(`/api/clinico/citas/${id}/posponer`, {
      nuevaFechaHora: datos.nuevaFechaHora,
      mensaje: datos.mensaje || null,
      observaciones: datos.observaciones || null
    });
  },

  async cumplirCita(id, datos) {
    return await clienteHttp.enviar(`/api/clinico/citas/${id}/cumplir`, {
      observaciones: datos.observaciones
    });
  },

  async cancelarCitaClinico(id, datos = {}) {
    return await clienteHttp.enviar(`/api/clinico/citas/${id}/cancelar`, {
      motivo: datos.motivo || datos.observaciones || null,
      observaciones: datos.observaciones || datos.motivo || null
    });
  }
};
