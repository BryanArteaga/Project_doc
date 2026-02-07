/**
 * Sistema de caché simple en memoria
 * Para reducir consultas a la base de datos en plan gratuito
 */

interface CacheEntry<T> {
  data: T;
  timestamp: number;
}

class CacheService {
  private cache: Map<string, CacheEntry<any>> = new Map();
  private defaultTTL: number = 5 * 60 * 1000; // 5 minutos por defecto

  /**
   * Obtener datos del caché o ejecutar la función si no existe/expiró
   */
  async get<T>(
    key: string,
    fetchFn: () => Promise<T>,
    ttl: number = this.defaultTTL
  ): Promise<T> {
    const cached = this.cache.get(key);
    const now = Date.now();

    // Si existe en caché y no ha expirado, retornar
    if (cached && (now - cached.timestamp) < ttl) {
      console.log(`[Cache HIT] ${key}`);
      return cached.data;
    }

    // Si no existe o expiró, obtener datos frescos
    console.log(`[Cache MISS] ${key}`);
    const data = await fetchFn();
    this.cache.set(key, { data, timestamp: now });

    return data;
  }

  /**
   * Invalidar una clave específica
   */
  invalidate(key: string): void {
    this.cache.delete(key);
    console.log(`[Cache INVALIDATE] ${key}`);
  }

  /**
   * Invalidar claves que coincidan con un patrón
   */
  invalidatePattern(pattern: string): void {
    const keys = Array.from(this.cache.keys());
    const regex = new RegExp(pattern);
    
    keys.forEach(key => {
      if (regex.test(key)) {
        this.cache.delete(key);
        console.log(`[Cache INVALIDATE] ${key}`);
      }
    });
  }

  /**
   * Limpiar todo el caché
   */
  clear(): void {
    this.cache.clear();
    console.log('[Cache CLEAR] Todo el caché eliminado');
  }

  /**
   * Obtener estadísticas del caché
   */
  getStats() {
    return {
      size: this.cache.size,
      keys: Array.from(this.cache.keys())
    };
  }
}

export const cacheService = new CacheService();
