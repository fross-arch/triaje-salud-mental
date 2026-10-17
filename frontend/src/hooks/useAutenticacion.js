import { useContext } from 'react';
import { ContextoAutenticacion } from '../contextos/ContextoAutenticacion.jsx';

export function useAutenticacion() {
  const contexto = useContext(ContextoAutenticacion);
  if (!contexto) {
    throw new Error('useAutenticacion debe ser utilizado dentro de un ProveedorAutenticacion');
  }
  return contexto;
}
