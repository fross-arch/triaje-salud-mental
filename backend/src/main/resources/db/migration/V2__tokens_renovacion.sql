-- Tokens de renovación de sesión. Solo se guarda el hash SHA-256 del token, nunca el token en claro.
-- Se eliminan en cascada junto con el usuario.
CREATE TABLE tokens_renovacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    hash_token VARCHAR(64) NOT NULL UNIQUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL,
    fecha_revocacion TIMESTAMP NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
