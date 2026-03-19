/**
 * Utility helpers para convertir tipos de Express Request que son string | string[]
 * a string único
 */

/**
 * Convierte un valor que puede ser string | string[] a string
 */
export const ensureString = (value: string | string[] | undefined, defaultValue = ''): string => {
  if (Array.isArray(value)) {
    return value[0] || defaultValue;
  }
  return value || defaultValue;
};

/**
 * Convierte un valor que puede ser string | string[] a número
 */
export const ensureNumber = (value: string | string[] | undefined, defaultValue = 0): number => {
  const str = ensureString(value, String(defaultValue));
  const num = parseInt(str, 10);
  return isNaN(num) ? defaultValue : num;
};
