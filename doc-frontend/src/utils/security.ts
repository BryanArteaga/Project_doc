/**
 * Utilidades de seguridad para el frontend
 */

// Sanitizar HTML para prevenir XSS
export const sanitizeHTML = (str: string): string => {
  const temp = document.createElement('div');
  temp.textContent = str;
  return temp.innerHTML;
};

// Validar email
export const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

// Validar cédula (exactamente 10 dígitos)
export const isValidCedula = (cedula: string): boolean => {
  const cedulaRegex = /^[0-9]{10}$/;
  return cedulaRegex.test(cedula);
};

// Validar teléfono (exactamente 10 dígitos)
export const isValidPhone = (phone: string): boolean => {
  const phoneRegex = /^[0-9]{10}$/;
  return phoneRegex.test(phone.replace(/[\s-]/g, ''));
};

// Validar contraseña fuerte
export const isStrongPassword = (password: string): { valid: boolean; message: string } => {
  if (password.length < 8) {
    return { valid: false, message: 'La contraseña debe tener al menos 8 caracteres' };
  }
  if (!/[A-Z]/.test(password)) {
    return { valid: false, message: 'La contraseña debe contener al menos una mayúscula' };
  }
  if (!/[a-z]/.test(password)) {
    return { valid: false, message: 'La contraseña debe contener al menos una minúscula' };
  }
  if (!/[0-9]/.test(password)) {
    return { valid: false, message: 'La contraseña debe contener al menos un número' };
  }
  return { valid: true, message: 'Contraseña válida' };
};

// Sanitizar input de texto
export const sanitizeInput = (input: string): string => {
  return input.trim().replace(/[<>]/g, '');
};

// Validar token JWT básico
export const isValidJWT = (token: string): boolean => {
  const parts = token.split('.');
  if (parts.length !== 3) return false;
  try {
    const payload = JSON.parse(atob(parts[1]));
    // Verificar que no esté expirado
    if (payload.exp && payload.exp * 1000 < Date.now()) {
      return false;
    }
    return true;
  } catch {
    return false;
  }
};

// Prevenir SQL injection básico (aunque el backend debe manejarlo)
export const preventSQLInjection = (input: string): string => {
  return input.replace(/['";\\]/g, '');
};

// Generar hash simple para verificación de integridad
export const simpleHash = async (str: string): Promise<string> => {
  const encoder = new TextEncoder();
  const data = encoder.encode(str);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
};

// Validar rango de valores numéricos
export const validateNumberRange = (value: number, min: number, max: number): boolean => {
  return value >= min && value <= max;
};

// Limpiar objeto de propiedades peligrosas
export const sanitizeObject = <T extends Record<string, any>>(obj: T): T => {
  const sanitized = { ...obj };
  for (const key in sanitized) {
    if (typeof sanitized[key] === 'string') {
      sanitized[key] = sanitizeInput(sanitized[key]) as any;
    }
  }
  return sanitized;
};

// Verificar permisos de acceso
export const hasPermission = (userRole: string, requiredRole: string): boolean => {
  const roleHierarchy: Record<string, number> = {
    'PATIENT': 1,
    'DOCTOR': 2,
    'ADMIN': 3
  };
  return (roleHierarchy[userRole] || 0) >= (roleHierarchy[requiredRole] || 0);
};

// Encriptar datos sensibles (básico, para storage local)
export const encryptData = (data: string, key: string): string => {
  // Implementación simple de XOR
  let encrypted = '';
  for (let i = 0; i < data.length; i++) {
    encrypted += String.fromCharCode(data.charCodeAt(i) ^ key.charCodeAt(i % key.length));
  }
  return btoa(encrypted);
};

// Desencriptar datos
export const decryptData = (encryptedData: string, key: string): string => {
  try {
    const data = atob(encryptedData);
    let decrypted = '';
    for (let i = 0; i < data.length; i++) {
      decrypted += String.fromCharCode(data.charCodeAt(i) ^ key.charCodeAt(i % key.length));
    }
    return decrypted;
  } catch {
    return '';
  }
};
