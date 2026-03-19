/**
 * Type overrides para Express Request
 * Esto resuelve el tipo string | string[] de req.params
 */

import 'express';

declare global {
  namespace Express {
    interface Request {
      /**
       * Los params siempre deben ser strings en este proyecto
       */
      params: Record<string, string>;
    }
  }
}

export {};
