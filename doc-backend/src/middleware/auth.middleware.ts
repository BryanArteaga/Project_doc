import { Request, Response, NextFunction } from 'express';
import { verifySupabaseToken } from '../lib/supabase';
import { prisma } from '../lib/prisma';
import jwt from 'jsonwebtoken';

// Extender tipos de Express
declare global {
  namespace Express {
    interface Request {
      userId?: number;
      userCedula?: string;
      estadoCuenta?: string;
      user?: any;
      doctorId?: string;
      doctorRole?: string;
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

    const secret = process.env.JWT_SECRET || 'your-secret-key';
    const payload = jwt.verify(token, secret) as any;
    
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

  if (req.estadoCuenta !== 'ACTIVE') {
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
    console.log('🔍 [verifyDoctorToken] Token recibido:', token ? 'SÍ' : 'NO');

    if (!token) {
      return res.status(401).json({ error: 'Token no proporcionado' });
    }

    // Verificar token con Supabase
    const user = await verifySupabaseToken(token);
    console.log('🔍 [verifyDoctorToken] Usuario Supabase:', user?.id);

    if (!user || !user.id) {
      return res.status(401).json({ error: 'Token inválido o expirado' });
    }

    // Buscar doctor en nuestra BD
    const doctor = await prisma.doctor.findUnique({
      where: { supabaseUserId: user.id }
    });

    console.log('🔍 [verifyDoctorToken] Doctor encontrado en BD:', doctor?.id, 'activo:', doctor?.activo);

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

    console.log('✅ [verifyDoctorToken] Middleware completado - doctorDbId:', req.doctorDbId);
    next();
  } catch (error: any) {
    console.error('❌ Error al verificar token de doctor:', error.message);
    return res.status(401).json({ error: 'No autenticado' });
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
 * Middleware para aceptar AMBOS: Doctor O Paciente
 * Intenta verificar como doctor primero, luego como paciente
 */
export const verifyAnyUser = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const token = req.headers.authorization?.split(' ')[1];

    if (!token) {
      return res.status(401).json({ error: 'Token no proporcionado' });
    }

    try {
      // Primero intentar verificar como DOCTOR
      const user = await verifySupabaseToken(token);

      if (user && user.id) {
        // Buscar doctor en BD
        const doctor = await prisma.doctor.findUnique({
          where: { supabaseUserId: user.id }
        });

        if (doctor && doctor.activo) {
          // Es doctor válido
          req.doctorId = user.id;
          req.doctorRole = doctor.rol;
          req.doctorDbId = doctor.id;
          console.log('✅ [verifyAnyUser] Autenticado como DOCTOR - doctorDbId:', doctor.id);
          return next();
        }
      }
    } catch (doctorError: any) {
      // No es doctor válido, intentar como paciente
      console.log('🔍 [verifyAnyUser] No es doctor, intentando como paciente...');
    }

    // Intentar verificar como PACIENTE (usando el token como JWT normal)
    const secret = process.env.JWT_SECRET || 'tu-secreto-aqui';
    try {
      const decoded: any = jwt.verify(token, secret);
      
      if (decoded && decoded.userId) {
        req.userId = decoded.userId;
        req.userCedula = decoded.cedula;
        console.log('✅ [verifyAnyUser] Autenticado como PACIENTE - userId:', decoded.userId);
        return next();
      }
    } catch (patientError: any) {
      console.log('❌ [verifyAnyUser] Token inválido para ambos tipos de usuario');
    }

    return res.status(401).json({ error: 'Token inválido - no se pudo autenticar como doctor ni paciente' });
  } catch (error: any) {
    console.error('❌ Error en verifyAnyUser:', error.message);
    return res.status(401).json({ error: 'No autenticado' });
  }
};

/**
 * Middleware combinado: Autenticación de doctor + rol ADMIN
 */
export const requireDoctorAdmin = [verifyDoctorToken, requireAdmin];
