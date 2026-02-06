import { Request, Response, NextFunction } from 'express';
import { AuthService } from '../services/auth.service';
import { AccountStatus } from '../types/prisma';
import { StaffRole } from '../../generated/prisma';
import { verifySupabaseToken } from '../lib/supabase';
import { prisma } from '../lib/prisma';

// Extender tipos de Express
declare global {
  namespace Express {
    interface Request {
      userId?: number;
      estadoCuenta?: AccountStatus;
      user?: any;
      doctorId?: string;
      doctorRole?: StaffRole;
      doctorDbId?: number;
    }
  }
}

/**
 * Middleware para verificar JWT token (solo para pacientes)
 */
export const verifyToken = (req: Request, res: Response, next: NextFunction) => {
  try {
    const token = req.headers.authorization?.split(' ')[1];

    if (!token) {
      return res.status(401).json({ error: 'Token no proporcionado' });
    }

    const payload = AuthService.verifyToken(token);
    if (!payload) {
      return res.status(401).json({ error: 'Token inválido o expirado' });
    }

    req.userId = payload.usuarioId;
    req.estadoCuenta = payload.estadoCuenta;

    next();
  } catch (error) {
    return res.status(401).json({ error: 'Error al verificar token' });
  }
};

/**
 * Middleware para verificar que la cuenta esté activa
 */
export const requireActive = (req: Request, res: Response, next: NextFunction) => {
  if (!req.estadoCuenta) {
    return res.status(401).json({ error: 'No autenticado' });
  }

  if (req.estadoCuenta !== AccountStatus.ACTIVE) {
    return res.status(403).json({
      error: 'Tu cuenta no está activada',
      estadoCuenta: req.estadoCuenta,
    });
  }

  next();
};

/**
 * Middleware para aplicar todas las validaciones (auth + active)
 */
export const requireAuth = [verifyToken, requireActive];

/**
 * Middleware legacy para compatibilidad
 */
export const authenticateToken = verifyToken;

/**
 * Middleware para verificar token de Supabase (doctores)
 */
export const verifyDoctorToken = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const token = req.headers.authorization?.split(' ')[1];

    if (!token) {
      return res.status(401).json({ error: 'Token no proporcionado' });
    }

    // Verificar token con Supabase
    const user = await verifySupabaseToken(token);

    if (!user || !user.id) {
      return res.status(401).json({ error: 'Token inválido o expirado' });
    }

    // Buscar doctor en nuestra BD
    const doctor = await prisma.doctor.findUnique({
      where: { supabaseUserId: user.id }
    });

    if (!doctor) {
      return res.status(404).json({ error: 'Doctor no encontrado en el sistema' });
    }

    if (!doctor.activo) {
      return res.status(403).json({ error: 'Cuenta desactivada' });
    }

    // Agregar info del doctor al request
    req.doctorId = user.id;
    req.doctorRole = doctor.rol;
    req.doctorDbId = doctor.id;

    next();
  } catch (error: any) {
    console.error('Error al verificar token de doctor:', error);
    return res.status(401).json({ error: 'Error al verificar token' });
  }
};

/**
 * Middleware para verificar que el doctor sea ADMIN
 */
export const requireAdmin = async (req: Request, res: Response, next: NextFunction) => {
  if (!req.doctorRole) {
    return res.status(401).json({ error: 'No autenticado' });
  }

  if (req.doctorRole !== 'ADMIN') {
    return res.status(403).json({ error: 'Acceso denegado: Se requiere rol de administrador' });
  }

  next();
};

/**
 * Middleware combinado: Autenticación de doctor
 */
export const requireDoctor = verifyDoctorToken;

/**
 * Middleware combinado: Autenticación de doctor + rol ADMIN
 */
export const requireDoctorAdmin = [verifyDoctorToken, requireAdmin];
