/**
 * Middleware para convertir todos los req.params a strings correctamente
 * Soluciona el tipo string | string[] de Express
 */

import { Request, Response, NextFunction } from 'express';

export const normalizeParamsMiddleware = (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  // Convertir todas las propiedades de params que sean arrays a string
  const normalized = req.params;
  
  Object.keys(normalized).forEach((key) => {
    const value = normalized[key];
    if (Array.isArray(value)) {
      normalized[key] = value[0] || '';
    }
  });

  next();
};
