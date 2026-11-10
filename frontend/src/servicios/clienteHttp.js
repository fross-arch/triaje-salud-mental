let tokenMemoria = null;
let escuchadoresCambioToken = [];

export function obtenerToken() {
  return tokenMemoria;
}

export function establecerToken(nuevoToken) {
  tokenMemoria = nuevoToken;
  escuchadoresCambioToken.forEach((fn) => fn(nuevoToken));
}

export function suscribirCambioToken(fn) {
  escuchadoresCambioToken.push(fn);
  return () => {
    escuchadoresCambioToken = escuchadoresCambioToken.filter((item) => item !== fn);
  };
}

let promesaRenovacionEnCurso = null;

async function renovarTokenInterno() {
  if (promesaRenovacionEnCurso) {
    return promesaRenovacionEnCurso;
  }

  promesaRenovacionEnCurso = (async () => {
    try {
      const respuesta = await fetch('/api/autenticacion/renovar', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (!respuesta.ok) {
        establecerToken(null);
        return null;
      }

      const datos = await respuesta.json();
      if (datos && datos.tokenAcceso) {
        establecerToken(datos.tokenAcceso);
        return datos.tokenAcceso;
      }
      establecerToken(null);
      return null;
    } catch (error) {
      establecerToken(null);
      return null;
    } finally {
      promesaRenovacionEnCurso = null;
    }
  })();

  return promesaRenovacionEnCurso;
}

export function redirigirNoAutorizado() {
  if (typeof window !== 'undefined' && window.location) {
    if (window.location.pathname !== '/no-autorizado') {
      try {
        if (typeof window.location.assign === 'function') {
          window.location.assign('/no-autorizado');
        } else {
          window.location.href = '/no-autorizado';
        }
      } catch {
        window.location.pathname = '/no-autorizado';
      }
    }
  }
}

export async function peticionHttp(ruta, opciones = {}) {
  const {
    metodo = 'GET',
    cuerpo = null,
    cabeceras = {},
    reintentar = true,
    esDescarga = false,
    silencioso = false,
    silenciosa = false
  } = opciones;

  const cabecerasFinales = {
    ...cabeceras
  };

  if (!(cuerpo instanceof FormData) && !esDescarga) {
    cabecerasFinales['Content-Type'] = 'application/json';
  }

  if (tokenMemoria) {
    cabecerasFinales['Authorization'] = `Bearer ${tokenMemoria}`;
  }

  const configuracion = {
    method: metodo,
    credentials: 'include',
    headers: cabecerasFinales
  };

  if (cuerpo && !(cuerpo instanceof FormData)) {
    configuracion.body = JSON.stringify(cuerpo);
  } else if (cuerpo) {
    configuracion.body = cuerpo;
  }

  let respuesta = await fetch(ruta, configuracion);

  // Manejo de renovacion automatica en caso de 401 si no estamos ya en la ruta de renovacion o inicio de sesion
  if (
    respuesta.status === 401 &&
    reintentar &&
    !ruta.includes('/api/autenticacion/renovar') &&
    !ruta.includes('/api/autenticacion/inicio-sesion')
  ) {
    const nuevoToken = await renovarTokenInterno();
    if (nuevoToken) {
      cabecerasFinales['Authorization'] = `Bearer ${nuevoToken}`;
      configuracion.headers = cabecerasFinales;
      respuesta = await fetch(ruta, configuracion);
    }
  }

  if (!respuesta.ok) {
    const esComprobacionSilenciosa = silencioso || silenciosa;
    if (respuesta.status === 403 && !esComprobacionSilenciosa) {
      redirigirNoAutorizado();
    }

    let mensajeError = `Error HTTP ${respuesta.status}`;
    try {
      const errorJson = await respuesta.json();
      if (errorJson && (errorJson.mensaje || errorJson.error)) {
        mensajeError = errorJson.mensaje || errorJson.error;
      }
    } catch {
      // Ignorar si el cuerpo del error no es JSON
    }
    const error = new Error(mensajeError);
    error.estado = respuesta.status;
    error.status = respuesta.status;
    error.mensaje = mensajeError;
    throw error;
  }

  if (respuesta.status === 204 || respuesta.headers?.get?.('content-length') === '0') {
    return null;
  }

  if (esDescarga) {
    return await respuesta.blob();
  }

  if (typeof respuesta.text === 'function') {
    const texto = await respuesta.text();
    if (!texto || texto.trim().length === 0) {
      return null;
    }
    try {
      return JSON.parse(texto);
    } catch {
      return texto;
    }
  }

  if (typeof respuesta.json === 'function') {
    try {
      return await respuesta.json();
    } catch {
      return null;
    }
  }

  return null;
}

export const clienteHttp = {
  obtener: (ruta, opciones = {}) => peticionHttp(ruta, { ...opciones, metodo: 'GET' }),
  enviar: (ruta, cuerpo, opciones = {}) => peticionHttp(ruta, { ...opciones, metodo: 'POST', cuerpo }),
  actualizar: (ruta, cuerpo, opciones = {}) => peticionHttp(ruta, { ...opciones, metodo: 'PUT', cuerpo }),
  eliminar: (ruta, opciones = {}) => peticionHttp(ruta, { ...opciones, metodo: 'DELETE' }),
  descargar: (ruta, opciones = {}) => peticionHttp(ruta, { ...opciones, metodo: 'GET', esDescarga: true }),
  descargarBlob: (ruta, opciones = {}) => peticionHttp(ruta, { ...opciones, metodo: 'GET', esDescarga: true }),
  renovarTokenInterno
};

