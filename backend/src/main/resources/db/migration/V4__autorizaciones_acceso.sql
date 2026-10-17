CREATE TABLE autorizaciones_acceso (
    id CHAR(36) PRIMARY KEY, -- UUID
    evaluacion_id CHAR(36) NOT NULL,
    paciente_id BIGINT NOT NULL,
    clinico_id BIGINT NOT NULL,
    version_texto_autorizacion VARCHAR(20) NOT NULL,
    fecha_otorgamiento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_revocacion TIMESTAMP NULL, -- NULL = vigente
    UNIQUE (evaluacion_id, clinico_id), -- volver a autorizar reactiva la misma fila
    FOREIGN KEY (evaluacion_id) REFERENCES evaluaciones(id) ON DELETE CASCADE,
    FOREIGN KEY (paciente_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (clinico_id) REFERENCES usuarios(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
