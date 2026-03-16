/**
 * Cache Management Utility
 * Maneja caché en localStorage con expiración
 */

interface CacheEntry<T> {
  data: T;
  timestamp: number;
  expiresIn: number; // en millisegundos
}

const CACHE_PREFIX = 'doc_cache_';

/**
 * Guardar datos en caché
 * @param key - Clave de caché
 * @param data - Datos a guardar
 * @param expiresInMinutes - Minutos hasta que expire (default: 30 minutos)
 */
export function setCache<T>(
  key: string,
  data: T,
  expiresInMinutes: number = 30
): void {
  try {
    const entry: CacheEntry<T> = {
      data,
      timestamp: Date.now(),
      expiresIn: expiresInMinutes * 60 * 1000
    };
    localStorage.setItem(`${CACHE_PREFIX}${key}`, JSON.stringify(entry));
  } catch (error) {
    console.warn('Error al guardar en caché:', error);
  }
}

/**
 * Obtener datos del caché
 * @param key - Clave de caché
 * @returns Datos del caché o null si está expirado/no existe
 */
export function getCache<T>(key: string): T | null {
  try {
    const item = localStorage.getItem(`${CACHE_PREFIX}${key}`);
    if (!item) return null;

    const entry: CacheEntry<T> = JSON.parse(item);
    const now = Date.now();
    const expiresAt = entry.timestamp + entry.expiresIn;

    // Verificar si ha expirado
    if (now > expiresAt) {
      clearCache(key);
      return null;
    }

    return entry.data;
  } catch (error) {
    console.warn('Error al obtener caché:', error);
    return null;
  }
}

/**
 * Limpiar caché específico
 * @param key - Clave de caché
 */
export function clearCache(key: string): void {
  try {
    localStorage.removeItem(`${CACHE_PREFIX}${key}`);
  } catch (error) {
    console.warn('Error al limpiar caché:', error);
  }
}

/**
 * Limpiar todo el caché de la app
 */
export function clearAllCache(): void {
  try {
    const keys = Object.keys(localStorage);
    keys.forEach(key => {
      if (key.startsWith(CACHE_PREFIX)) {
        localStorage.removeItem(key);
      }
    });
  } catch (error) {
    console.warn('Error al limpiar todo el caché:', error);
  }
}

/**
 * Verificar si el caché existe y no ha expirado
 * @param key - Clave de caché
 */
export function hasValidCache(key: string): boolean {
  return getCache(key) !== null;
}
