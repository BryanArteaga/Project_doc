-- ========================================
-- SETUP INICIAL - ÍNDICES Y FUNCIONES
-- Ejecutar UNA SOLA VEZ al configurar Supabase
-- ========================================

-- ========================================
-- 1. ÍNDICES PARA MEJORAR PERFORMANCE
-- ========================================

-- Índice en usuarios.idDoctorAsignado para búsquedas de pacientes por doctor
CREATE INDEX IF NOT EXISTS idx_usuario_doctor_id 
ON usuarios ("idDoctorAsignado") 
WHERE "estadoCuenta" = 'ACTIVE';

-- Índice en usuarios.estadoCuenta para filtros
CREATE INDEX IF NOT EXISTS idx_usuario_estado 
ON usuarios ("estadoCuenta");

-- Índice en usuarios.email para login
CREATE INDEX IF NOT EXISTS idx_usuario_email 
ON usuarios (email);

-- Índice en registros_salud.usuarioId para búsquedas de registros por paciente
CREATE INDEX IF NOT EXISTS idx_registro_salud_usuario_id 
ON registros_salud ("usuarioId");

-- Índice en registros_salud.fecha para ordenamiento y filtrado
CREATE INDEX IF NOT EXISTS idx_registro_salud_fecha 
ON registros_salud (fecha DESC);

-- Índice en registros_salud.registradoEn para queries por timestamp
CREATE INDEX IF NOT EXISTS idx_registro_salud_registrado 
ON registros_salud ("registradoEn" DESC);

-- Índice en ejercicios_realizados.usuarioId
CREATE INDEX IF NOT EXISTS idx_ejercicio_usuario_id 
ON ejercicios_realizados ("usuarioId");

-- Índice en ejercicios_realizados.fecha para ordenamiento
CREATE INDEX IF NOT EXISTS idx_ejercicio_fecha 
ON ejercicios_realizados (fecha DESC);

-- Índice en ejercicios_realizados.completadoEn para queries por timestamp
CREATE INDEX IF NOT EXISTS idx_ejercicio_completado 
ON ejercicios_realizados ("completadoEn" DESC);

-- Índice en doctores.email para login
CREATE INDEX IF NOT EXISTS idx_doctor_email 
ON doctores (email);

-- Índice en doctores.cedula para búsquedas
CREATE INDEX IF NOT EXISTS idx_doctor_cedula 
ON doctores (cedula);

-- Índice en doctores.activo para filtros
CREATE INDEX IF NOT EXISTS idx_doctor_activo 
ON doctores (activo) 
WHERE activo = true;

-- ========================================
-- 2. FUNCIONES DE MANTENIMIENTO
-- ========================================

-- Función para limpiar tokens expirados de auth
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS TABLE (
  deleted_refresh_tokens bigint,
  deleted_sessions bigint,
  deleted_one_time_tokens bigint
) AS $$
DECLARE
  v_refresh_tokens bigint;
  v_sessions bigint;
  v_one_time_tokens bigint;
BEGIN
  -- Limpiar refresh tokens antiguos (más de 7 días)
  DELETE FROM auth.refresh_tokens 
  WHERE created_at < NOW() - INTERVAL '7 days';
  GET DIAGNOSTICS v_refresh_tokens = ROW_COUNT;
  
  -- Limpiar sesiones antiguas (más de 7 días)
  DELETE FROM auth.sessions 
  WHERE created_at < NOW() - INTERVAL '7 days';
  GET DIAGNOSTICS v_sessions = ROW_COUNT;
  
  -- Limpiar one-time tokens antiguos (más de 1 día)
  DELETE FROM auth.one_time_tokens 
  WHERE created_at < NOW() - INTERVAL '1 day';
  GET DIAGNOSTICS v_one_time_tokens = ROW_COUNT;
  
  RETURN QUERY SELECT v_refresh_tokens, v_sessions, v_one_time_tokens;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ========================================
-- 3. FUNCIÓN PARA MONITOREO DE DB
-- ========================================

-- Función para obtener estadísticas de la base de datos
CREATE OR REPLACE FUNCTION get_database_stats()
RETURNS TABLE (
  metric text,
  value text
) AS $$
BEGIN
  RETURN QUERY
  SELECT 'active_connections'::text, 
         count(*)::text 
  FROM pg_stat_activity 
  WHERE datname = current_database()
  
  UNION ALL
  
  SELECT 'database_size'::text,
         pg_size_pretty(pg_database_size(current_database()))
  
  UNION ALL
  
  SELECT 'largest_table'::text,
         COALESCE((SELECT tablename FROM pg_tables WHERE schemaname = 'public' 
          ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC LIMIT 1), 'N/A')
  
  UNION ALL
  
  SELECT 'total_tables'::text,
         count(*)::text
  FROM pg_tables
  WHERE schemaname = 'public'
  
  UNION ALL
  
  SELECT 'total_indexes'::text,
         count(*)::text
  FROM pg_indexes
  WHERE schemaname = 'public';
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- 4. VERIFICACIÓN DE ÍNDICES
-- ========================================

-- Ver índices creados exitosamente
SELECT
  schemaname,
  tablename,
  indexname,
  pg_size_pretty(pg_relation_size(indexname::regclass)) as size
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname LIKE 'idx_%'
ORDER BY tablename, indexname;

-- Ver estadísticas de la base de datos
SELECT * FROM get_database_stats();
