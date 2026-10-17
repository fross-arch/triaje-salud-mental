import { clienteHttp } from './clienteHttp.js';

export const servicioAdministracion = {
  async listarClinicos() {
    return await clienteHttp.obtener('/api/administracion/clinicos');
  },

  async crearClinico(datos) {
    return await clienteHttp.enviar('/api/administracion/clinicos', {
      nombreCompleto: datos.nombreCompleto,
      correo: datos.correo,
      contrasenaInicial: datos.contrasenaInicial,
      especialidad: datos.especialidad || null
    });
  },

  async actualizarClinico(id, datos) {
    const payload = {
      nombreCompleto: datos.nombreCompleto,
      correo: datos.correo,
      especialidad: datos.especialidad || null
    };
    if (datos.nuevaContrasena) {
      payload.nuevaContrasena = datos.nuevaContrasena;
    }
    return await clienteHttp.actualizar(`/api/administracion/clinicos/${id}`, payload);
  },

  async eliminarClinico(id) {
    return await clienteHttp.eliminar(`/api/administracion/clinicos/${id}`);
  },

  async activarClinico(id) {
    return await clienteHttp.enviar(`/api/administracion/clinicos/${id}/activar`, {});
  },

  async desactivarClinico(id) {
    return await clienteHttp.enviar(`/api/administracion/clinicos/${id}/desactivar`, {});
  },

  async listarPacientes() {
    return await clienteHttp.obtener('/api/administracion/pacientes');
  },

  async actualizarPaciente(id, datos) {
    const payload = {
      nombreCompleto: datos.nombreCompleto,
      correo: datos.correo
    };
    if (datos.nuevaContrasena) {
      payload.nuevaContrasena = datos.nuevaContrasena;
    }
    return await clienteHttp.actualizar(`/api/administracion/pacientes/${id}`, payload);
  }
};
