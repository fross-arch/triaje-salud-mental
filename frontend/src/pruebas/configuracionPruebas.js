import '@testing-library/jest-dom';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

// Limpieza automatica del DOM despues de cada prueba
afterEach(() => {
  cleanup();
});

// Silenciar advertencias futuras de React Router v7 en consola durante pruebas
const advertenciaOriginal = console.warn;
console.warn = (...args) => {
  if (
    typeof args[0] === 'string' &&
    args[0].includes('React Router Future Flag Warning')
  ) {
    return;
  }
  advertenciaOriginal(...args);
};

