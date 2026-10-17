CREATE TABLE citas (
    id CHAR(36) PRIMARY KEY, -- UUID
    autorizacion_id CHAR(36) NOT NULL,
    paciente_id BIGINT NOT NULL,
    clinico_id BIGINT NOT NULL,
    fecha_hora TIMESTAMP NOT NULL,
    modalidad ENUM('PRESENCIAL', 'VIRTUAL') NOT NULL,
    mensaje VARCHAR(300) NULL, -- sin datos clínicos
    estado ENUM('PROPUESTA', 'CONFIRMADA', 'RECHAZADA', 'CANCELADA') NOT NULL DEFAULT 'PROPUESTA',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_citas_clinico_fecha (clinico_id, fecha_hora),
    FOREIGN KEY (autorizacion_id) REFERENCES autorizaciones_acceso(id) ON DELETE CASCADE,
    FOREIGN KEY (paciente_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (clinico_id) REFERENCES usuarios(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
