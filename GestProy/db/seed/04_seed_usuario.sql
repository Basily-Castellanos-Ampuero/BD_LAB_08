-- ============================================================
--  GestProy — Datos semilla
--  04_seed_usuario.sql : cuenta única de administración ("admi").
--
--  Solo se guarda el HASH (Blowfish, pgcrypto), nunca la
--  contraseña en texto plano — ni siquiera en este script.
--  Contraseña inicial de referencia para el entorno de
--  desarrollo: "testpass123". Cámbiala antes de cualquier uso
--  real con:
--    UPDATE g1s_usuario
--    SET usu_pass_hash = crypt('tu_nueva_clave', gen_salt('bf'))
--    WHERE usu_login = 'admi';
-- ============================================================
BEGIN;

INSERT INTO g1s_usuario (usu_login, usu_pass_hash, usu_est_reg_cod)
VALUES ('admi', '$2a$06$2qvGhj3bJxIUwlj9nfXs/OzRPe5RkAJ0UdeqRVprweEym7cX0k25W', 'A')
ON CONFLICT (usu_login) DO NOTHING;

COMMIT;
