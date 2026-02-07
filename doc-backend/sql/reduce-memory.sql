-- ===================================================
-- OPTIMIZACIONES AGRESIVAS PARA REDUCIR MEMORIA
-- ===================================================
-- IMPORTANTE: Ejecutar cada sección por separado en Supabase SQL Editor
-- VACUUM y REINDEX no pueden ejecutarse en bloques de transacción
-- ===================================================

-- ===================================================
-- SECCIÓN 1: ANÁLISIS Y ESTADÍSTICAS (Ejecutar primero)
-- ===================================================
-- Puedes ejecutar esta sección completa de una vez

ANALYZE usuarios;
ANALYZE doctores;
ANALYZE registros_salud;
ANALYZE ejercicios_realizados;

-- ===================================================
-- SECCIÓN 2: LIMPIEZA DE CONEXIONES (Ejecutar segunda)
-- ===================================================
-- Puedes ejecutar esta sección completa de una vez

-- Ver conexiones inactivas (solo información)
SELECT 
  pid,
  usename,
  application_name,
  state,
  state_change,
  NOW() - state_change as idle_time
FROM pg_stat_activity
WHERE datname = current_database()
  AND state = 'idle'
  AND state_change < NOW() - INTERVAL '30 minutes'
  AND pid <> pg_backend_pid()
ORDER BY state_change;

-- Terminar conexiones inactivas (solo si no son superusuarios)
-- NOTA: Puede fallar si hay procesos de superusuario
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = current_database()
  AND state = 'idle'
  AND state_change < NOW() - INTERVAL '30 minutes'
  AND pid <> pg_backend_pid()
  AND NOT EXISTS (
    SELECT 1 FROM pg_roles 
    WHERE rolname = usename AND rolsuper = true
  );


-- ===================================================
-- SECCIÓN 3: VACUUM (Ejecutar UNO POR UNO)
-- ===================================================
-- IMPORTANTE: Cada comando VACUUM debe ejecutarse por separado
-- Copia y pega cada línea individualmente en SQL Editor

-- VACUUM FULL usuarios;
-- VACUUM FULL doctores;
-- VACUUM FULL registros_salud;
-- VACUUM FULL ejercicios_realizados;


-- ===================================================
-- SECCIÓN 4: REINDEX (Ejecutar UNO POR UNO)
-- ===================================================
-- IMPORTANTE: Cada comando REINDEX debe ejecutarse por separado
-- Copia y pega cada línea individualmente en SQL Editor

-- REINDEX TABLE usuarios;
-- REINDEX TABLE doctores;
-- REINDEX TABLE registros_salud;
-- REINDEX TABLE ejercicios_realizados;


-- ===================================================
-- SECCIÓN 5: CONSULTAS DE MONITOREO (Ejecutar al final)
-- ===================================================
-- Puedes ejecutar estas queries para ver el resultado

-- Ver conexiones activas
SELECT 
  pid,
  usename,
  application_name,
  state,
  query_start,
  NOW() - query_start as query_duration,
  state_change
FROM pg_stat_activity
WHERE datname = current_database()
ORDER BY query_start DESC NULLS LAST;

-- Ver estadísticas de caché
SELECT 
  heap_blks_read as disk_reads,
  heap_blks_hit as cache_hits,
  round(heap_blks_hit::numeric / (heap_blks_hit + heap_blks_read) * 100, 2) as cache_hit_ratio
FROM pg_statio_user_tables
WHERE heap_blks_hit + heap_blks_read > 0
ORDER BY cache_hit_ratio DESC;

-- Ver tamaños de tablas e índices
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
  pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as indexes_size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
