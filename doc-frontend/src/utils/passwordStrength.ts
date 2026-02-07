// Validación de texto (solo letras y espacios)
export const isValidTextOnly = (text: string): boolean => {
  const textRegex = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/;
  return textRegex.test(text);
};

// Calcular nivel de fortaleza de contraseña
export interface PasswordStrength {
  level: 'muy-debil' | 'debil' | 'medio' | 'fuerte' | 'muy-fuerte';
  score: number; // 0-100
  message: string;
  color: string;
  suggestions: string[];
}

export const getPasswordStrength = (password: string): PasswordStrength => {
  let score = 0;
  const suggestions: string[] = [];

  // Longitud
  if (password.length >= 8) score += 20;
  else suggestions.push('Usa al menos 8 caracteres');
  
  if (password.length >= 12) score += 10;
  if (password.length >= 16) score += 10;

  // Minúsculas
  if (/[a-z]/.test(password)) {
    score += 15;
  } else {
    suggestions.push('Agrega letras minúsculas');
  }

  // Mayúsculas
  if (/[A-Z]/.test(password)) {
    score += 15;
  } else {
    suggestions.push('Agrega letras mayúsculas');
  }

  // Números
  if (/[0-9]/.test(password)) {
    score += 15;
  } else {
    suggestions.push('Agrega números');
  }

  // Caracteres especiales
  if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
    score += 15;
  } else {
    suggestions.push('Agrega caracteres especiales (!@#$%...)');
  }

  // Variedad de caracteres
  const uniqueChars = new Set(password).size;
  if (uniqueChars >= 8) score += 10;

  // Determinar nivel y mensaje
  let level: PasswordStrength['level'];
  let message: string;
  let color: string;

  if (score < 30) {
    level = 'muy-debil';
    message = 'Muy débil';
    color = '#dc2626'; // rojo
  } else if (score < 50) {
    level = 'debil';
    message = 'Débil';
    color = '#f97316'; // naranja
  } else if (score < 70) {
    level = 'medio';
    message = 'Media';
    color = '#f59e0b'; // amarillo
  } else if (score < 90) {
    level = 'fuerte';
    message = 'Fuerte';
    color = '#84cc16'; // verde claro
  } else {
    level = 'muy-fuerte';
    message = 'Muy fuerte';
    color = '#16a34a'; // verde oscuro
  }

  return {
    level,
    score,
    message,
    color,
    suggestions: suggestions.slice(0, 3) // Máximo 3 sugerencias
  };
};
