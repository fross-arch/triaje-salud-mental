import { clienteHttp, establecerToken } from './clienteHttp.js';

export const servicioAutenticacion = {
  async iniciarSesion(correo, contrasena) {
    const respuesta = await clienteHttp.enviar('/api/autenticacion/inicio-sesion', {
      correo,
      contrasena
    });
    if (respuesta && respuesta.tokenAcceso) {
      establecerToken(respuesta.tokenAcceso);
    }
    return respuesta;
  },

  async registrar(nombreCompleto, correo, contrasena) {
    return await clienteHttp.enviar('/api/autenticacion/registro', {
      nombreCompleto,
      correo,
      contrasena
    });
  },

  async renovar() {
    const respuesta = await clienteHttp.enviar('/api/autenticacion/renovar', {}, { silencioso: true });
    if (respuesta && respuesta.tokenAcceso) {
      establecerToken(respuesta.tokenAcceso);
    }
    return respuesta;
  },

  async cerrarSesion() {
    try {
      await clienteHttp.enviar('/api/autenticacion/cierre-sesion', {});
    } finally {
      establecerToken(null);
    }
  },

  async obtenerPerfil() {
    return await clienteHttp.obtener('/api/usuarios/yo');
  },

  async actualizarPerfil(datos) {
    return await clienteHttp.actualizar('/api/usuarios/yo', datos);
  },

  async eliminarCuenta() {
    try {
      await clienteHttp.eliminar('/api/usuarios/yo');
    } finally {
      establecerToken(null);
    }
  }
};
