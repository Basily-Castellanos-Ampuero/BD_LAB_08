-- ============================================================
--  GestProy — Esquema PostgreSQL
--  06_usuario.sql : cuenta de administración del panel web.
--
--  El sistema completo tiene UNA sola cuenta con permiso de
--  escritura ("admi"). No hay registro de usuarios ni roles:
--  esta tabla existe solo para poder verificar login+contraseña
--  sin guardar la clave en texto plano.
--
--  Requiere: 01_referenciales.sql (gzz_est_reg) y la extensión
--  pgcrypto (se habilita aquí mismo) para el hash de contraseñas.
-- ============================================================
BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP TABLE IF EXISTS g1s_usuario CASCADE;
CREATE TABLE g1s_usuario (
  usu_login       VARCHAR(30) NOT NULL,
  -- hash Blowfish (pgcrypto crypt/gen_salt('bf')); nunca texto plano
  usu_pass_hash   TEXT        NOT NULL,
  usu_est_reg_cod CHAR(1)     NOT NULL,
  PRIMARY KEY (usu_login),
  CONSTRAINT fk_usuario_estreg FOREIGN KEY (usu_est_reg_cod)
    REFERENCES gzz_est_reg (est_reg_cod)
);

COMMIT;
