CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(120) NOT NULL,
    correo VARCHAR(120) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(255) NOT NULL,
    rol ENUM('PACIENTE', 'CLINICO') NOT NULL DEFAULT 'PACIENTE', -- CLINICO reservado, sin endpoints en el MVP
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE consentimientos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    version_consentimiento VARCHAR(20) NOT NULL,
    version_politica VARCHAR(20) NOT NULL,
    confirma_mayoria_edad BOOLEAN NOT NULL,
    fecha_aceptacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_revocacion TIMESTAMP NULL,
    CHECK (confirma_mayoria_edad = TRUE),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE evaluaciones (
    id CHAR(36) PRIMARY KEY, -- UUID
    usuario_id BIGINT NOT NULL,
    estado ENUM('EN_CURSO', 'COMPLETADA', 'ABANDONADA') NOT NULL DEFAULT 'EN_CURSO',
    puntaje_phq9 TINYINT NULL,
    puntaje_gad7 TINYINT NULL,
    severidad_depresion ENUM('MINIMA','LEVE','MODERADA','MODERADAMENTE_SEVERA','SEVERA') NULL,
    severidad_ansiedad ENUM('MINIMA','LEVE','MODERADA','SEVERA') NULL,
    riesgo_crisis BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_finalizacion TIMESTAMP NULL,
    CHECK (puntaje_phq9 BETWEEN 0 AND 27),
    CHECK (puntaje_gad7 BETWEEN 0 AND 21),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE respuestas_evaluacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluacion_id CHAR(36) NOT NULL,
    instrumento ENUM('PHQ9', 'GAD7') NOT NULL,
    indice_pregunta TINYINT NOT NULL, -- PHQ9: 1-9, GAD7: 1-7 (se valida también en el dominio)
    valor_puntaje TINYINT NOT NULL,
    CHECK (indice_pregunta BETWEEN 1 AND 9),
    CHECK (valor_puntaje BETWEEN 0 AND 3),
    UNIQUE (evaluacion_id, instrumento, indice_pregunta),
    FOREIGN KEY (evaluacion_id) REFERENCES evaluaciones(id) ON DELETE CASCADE
);

CREATE TABLE registro_auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NULL,
    accion VARCHAR(50) NOT NULL, -- INICIO_SESION, CONSENTIMIENTO_ACEPTADO, CONSENTIMIENTO_REVOCADO, EVALUACION_COMPLETADA, PDF_DESCARGADO, CUENTA_ELIMINADA
    entidad_id VARCHAR(36) NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL
);
