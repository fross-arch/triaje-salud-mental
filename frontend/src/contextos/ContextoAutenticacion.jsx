import React, { createContext, useState, useEffect, useCallback } from 'react';
import { servicioAutenticacion } from '../servicios/servicioAutenticacion.js';
import { establecerToken, suscribirCambioToken } from '../servicios/clienteHttp.js';

export const ContextoAutenticacion = createContext(null);

export function ProveedorAutenticacion({ children }) {
  // El token JWT vive exclusivamente en la memoria de React (nunca en localStorage ni sessionStorage)
  const [tokenAcceso, setTokenAcceso] = useState(null);
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [errorAutenticacion, setErrorAutenticacion] = useState(null);

  // Sincronizar estado local con el cliente HTTP
  useEffect(() => {
    const desuscribir = suscribirCambioToken((nuevoToken) => {
      setTokenAcceso(nuevoToken);
    });
    return desuscribir;
  }, []);

  const cargarPerfilUsuario = useCallback(async () => {
    try {
      const perfil = await servicioAutenticacion.obtenerPerfil();
      setUsuario(perfil);
      return perfil;
    } catch {
      setUsuario(null);
      return null;
    }
  }, []);

  // Al montar la aplicacion, intenta renovar silenciosamente mediante la cookie httpOnly
  useEffect(() => {
    let montado = true;

    async function verificarSesionInicial() {
      try {
        const respuesta = await servicioAutenticacion.renovar();
        if (montado && respuesta && respuesta.tokenAcceso) {
          establecerToken(respuesta.tokenAcceso);
          setTokenAcceso(respuesta.tokenAcceso);
          await cargarPerfilUsuario();
        } else if (montado) {
          establecerToken(null);
          setTokenAcceso(null);
          setUsuario(null);
        }
      } catch (error) {
        // En caso de 401 (Unauthorized) o ausencia de cookie durante la verificación pasiva inicial:
        // - No registrar errores en la consola con console.error
        // - Establecer token y usuario como null
        // - No redirigir al usuario
        if (montado) {
          establecerToken(null);
          setTokenAcceso(null);
          setUsuario(null);
        }
      } finally {
        if (montado) {
          setCargando(false);
        }
      }
    }

    verificarSesionInicial();

    return () => {
      montado = false;
    };
  }, [cargarPerfilUsuario]);

  const iniciarSesion = async (correo, contrasena) => {
    setErrorAutenticacion(null);
    try {
      const resultado = await servicioAutenticacion.iniciarSesion(correo, contrasena);
      establecerToken(resultado.tokenAcceso);
      setTokenAcceso(resultado.tokenAcceso);
      const perfil = await cargarPerfilUsuario();
      return { exito: true, usuario: perfil };
    } catch (error) {
      setErrorAutenticacion(error.message || 'Error al iniciar sesion');
      throw error;
    }
  };

  const registrar = async (nombreCompleto, correo, contrasena) => {
    setErrorAutenticacion(null);
    try {
      const nuevoUsuario = await servicioAutenticacion.registrar(nombreCompleto, correo, contrasena);
      // Tras registrarse, se inicia sesion automaticamente
      await iniciarSesion(correo, contrasena);
      return nuevoUsuario;
    } catch (error) {
      setErrorAutenticacion(error.message || 'Error al registrar usuario');
      throw error;
    }
  };

  const cerrarSesion = async () => {
    try {
      await servicioAutenticacion.cerrarSesion();
    } finally {
      establecerToken(null);
      setTokenAcceso(null);
      setUsuario(null);
    }
  };

  const eliminarCuenta = async () => {
    try {
      await servicioAutenticacion.eliminarCuenta();
    } finally {
      establecerToken(null);
      setTokenAcceso(null);
      setUsuario(null);
    }
  };

  const actualizarUsuarioEnEstado = (nuevoUsuario) => {
    setUsuario((prev) => (prev ? { ...prev, ...nuevoUsuario } : nuevoUsuario));
  };

  const valor = {
    tokenAcceso,
    usuario,
    cargando,
    errorAutenticacion,
    estaAutenticado: Boolean(tokenAcceso && usuario),
    iniciarSesion,
    registrar,
    cerrarSesion,
    eliminarCuenta,
    cargarPerfilUsuario,
    actualizarUsuarioEnEstado
  };

  return (
    <ContextoAutenticacion.Provider value={valor}>
      {children}
    </ContextoAutenticacion.Provider>
  );
}
