-- ============================================================
--  GestProy — Funciones de autenticación
--  fn_usuario_autenticar : verifica login+contraseña contra el
--  hash almacenado en g1s_usuario.
--
--  Función de LECTURA (LANGUAGE sql STABLE): no escribe nada,
--  solo compara. crypt(p_pass, usu_pass_hash) vuelve a hashear
--  la contraseña recibida usando la MISMA sal que ya quedó
--  codificada dentro de usu_pass_hash (así es como funciona
--  pgcrypto con Blowfish) y compara el resultado.
--
--  Devuelve TRUE solo si el usuario existe, está activo
--  (usu_est_reg_cod = 'A') y el hash coincide.
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION fn_usuario_autenticar(
  p_login TEXT,
  p_pass  TEXT
)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM g1s_usuario u
    WHERE u.usu_login = p_login
      AND u.usu_est_reg_cod = 'A'
      AND u.usu_pass_hash = crypt(p_pass, u.usu_pass_hash)
  );
$$;

COMMIT;
