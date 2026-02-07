-- ========================================
-- MANTENIMIENTO MANUAL
-- Ejecutar manualmente cuando sea necesario
-- ========================================

-- ========================================
-- 1. LIMPIEZA DE TOKENS EXPIRADOS
-- ========================================

-- Ejecutar limpieza de tokens (ejecutar manualmente cuando sea necesario)
-- Devuelve el número de registros eliminados
SELECT * FROM cleanup_expired_tokens();

-- ========================================
-- 2. ESTADÍSTICAS DE BASE DE DATOS
-- ========================================

-- Ver estadísticas actuales
SELECT * FROM get_database_stats();

-- ========================================
-- 3. MONITOREO DE TABLAS
-- ========================================

-- Ver tamaño de cada tabla
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
  pg_total_relation_size(schemaname||'.'||tablename) as bytes
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- ========================================
-- 4. MONITOREO DE CONEXIONES
-- ========================================

-- Ver conexiones activas
SELECT 
  pid,
  usename,
  application_name,
  client_addr,
  state,
  query_start,
  state_change,
  NOW() - state_change as idle_time
FROM pg_stat_activity
WHERE datname = current_database()
ORDER BY state_change DESC;

-- Contar conexiones por estado
SELECT 
  state,
  count(*) as connections
FROM pg_stat_activity
WHERE datname = current_database()
GROUP BY state
ORDER BY connections DESC;

-- ========================================
-- 5. MONITOREO DE ÍNDICES
-- ========================================

-- Ver índices existentes y su tamaño
SELECT
  schemaname,
  tablename,
  indexname,
  pg_size_pretty(pg_relation_size(indexname::regclass)) as size,
  pg_relation_size(indexname::regclass) as bytes
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY pg_relation_size(indexname::regclass) DESC;

-- Ver uso de índices
SELECT
  schemaname,
  tablename,
  indexname,
  idx_scan as index_scans,
  idx_tup_read as tuples_read,
  idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- ========================================
-- 6. VACUUM Y ANÁLISIS (Opcional)
-- ========================================

-- Liberar espacio y actualizar estadísticas
-- Ejecutar ocasionalmente para optimizar performance
-- Descomenta las líneas que necesites ejecutar:

-- VACUUM ANALYZE usuarios;
-- VACUUM ANALYZE doctores;
-- VACUUM ANALYZE registros_salud;
-- VACUUM ANALYZE ejercicios_realizados;

-- O ejecutar en todas las tablas del schema público:
-- VACUUM ANALYZE;

-- ========================================
-- 7. LIMPIEZA DE REGISTROS ANTIGUOS (CUIDADO)
-- ========================================

-- SOLO ejecutar si necesitas liberar espacio
-- Reemplaza YYYY-MM-DD con la fecha límite

-- Ver cuántos registros hay antes de una fecha
-- SELECT count(*) FROM registros_salud 
-- WHERE fecha < '2025-01-01';

-- Eliminar registros de salud antiguos (DESCOMENTAR CON PRECAUCIÓN)
-- DELETE FROM registros_salud 
-- WHERE fecha < '2025-01-01';

-- Eliminar ejercicios antiguos (DESCOMENTAR CON PRECAUCIÓN)
-- DELETE FROM ejercicios_realizados 
-- WHERE fecha < '2025-01-01';
