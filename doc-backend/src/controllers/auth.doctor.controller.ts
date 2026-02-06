import { Request, Response } from 'express';
import { AuthDoctorService } from '../services/auth.doctor.service';
import { z } from 'zod';

// Schemas de validación
const RegisterDoctorSchema = z.object({
  email: z.string().email('Email inválido'),
  password: z.string().min(8, 'Password debe tener al menos 8 caracteres'),
  nombre: z.string().min(2, 'Nombre debe tener al menos 2 caracteres'),
  apellido: z.string().min(2, 'Apellido debe tener al menos 2 caracteres'),
  cedula: z.string().min(5, 'Cédula inválida'),
  telefono: z.string().optional(),
  especialidad: z.string().optional(),
  rol: z.enum(['ADMIN', 'DOCTOR']).optional(),
});

const LoginDoctorSchema = z.object({
  email: z.string().email('Email inválido'),
  password: z.string().min(6, 'Password debe tener al menos 6 caracteres'),
});

const CreateFirstAdminSchema = z.object({
  email: z.string().email('Email inválido'),
  password: z.string().min(8, 'Password debe tener al menos 8 caracteres'),
  nombre: z.string().min(2, 'Nombre debe tener al menos 2 caracteres'),
  apellido: z.string().min(2, 'Apellido debe tener al menos 2 caracteres'),
  cedula: z.string().min(5, 'Cédula inválida'),
  telefono: z.string().optional(),
  especialidad: z.string().optional(),
});

/**
 * Registrar nuevo doctor (solo ADMIN)
 */
export const registerDoctor = async (req: Request, res: Response) => {
  try {
    if (!req.doctorId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const data = RegisterDoctorSchema.parse(req.body);
    const doctor = await AuthDoctorService.register(data, req.doctorId);

    res.status(201).json({
      message: 'Doctor registrado exitosamente',
      doctor,
    });
  } catch (error: any) {
    console.error('Error en registro de doctor:', error);

    if (error instanceof z.ZodError) {
      return res.status(400).json({
        error: 'Validación fallida',
        details: error.errors,
      });
    }

    res.status(400).json({
      error: error.message || 'Error al registrar doctor',
    });
  }
};

/**
 * Login de doctor
 */
export const loginDoctor = async (req: Request, res: Response) => {
  try {
    const data = LoginDoctorSchema.parse(req.body);
    const result = await AuthDoctorService.login(data);

    res.json({
      message: 'Login exitoso',
      token: result.token,
      refreshToken: result.refreshToken,
      doctor: result.doctor,
    });
  } catch (error: any) {
    console.error('Error en login de doctor:', error);

    if (error instanceof z.ZodError) {
      return res.status(400).json({
        error: 'Validación fallida',
        details: error.errors,
      });
    }

    res.status(400).json({
      error: error.message || 'Error al iniciar sesión',
    });
  }
};

/**
 * Obtener doctor actual (info del doctor logueado)
 */
export const getCurrentDoctor = async (req: Request, res: Response) => {
  try {
    if (!req.doctorId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const doctor = await AuthDoctorService.getDoctorBySupabaseId(req.doctorId);

    res.json({
      doctor,
    });
  } catch (error: any) {
    console.error('Error al obtener doctor:', error);

    res.status(400).json({
      error: error.message || 'Error al obtener información del doctor',
    });
  }
};

/**
 * Logout de doctor
 */
export const logoutDoctor = async (req: Request, res: Response) => {
  try {
    const token = req.headers.authorization?.replace('Bearer ', '');

    if (!token) {
      return res.status(400).json({ error: 'Token no proporcionado' });
    }

    await AuthDoctorService.logout(token);

    res.json({
      message: 'Sesión cerrada exitosamente',
    });
  } catch (error: any) {
    console.error('Error en logout:', error);

    res.status(500).json({
      error: error.message || 'Error al cerrar sesión',
    });
  }
};

/**
 * Crear primer administrador del sistema
 * Esta ruta debe ser protegida o usarse solo una vez
 */
export const createFirstAdmin = async (req: Request, res: Response) => {
  try {
    const data = CreateFirstAdminSchema.parse(req.body);
    const admin = await AuthDoctorService.createFirstAdmin(data);

    res.status(201).json({
      message: 'Administrador creado exitosamente',
      admin,
    });
  } catch (error: any) {
    console.error('Error al crear primer admin:', error);

    if (error instanceof z.ZodError) {
      return res.status(400).json({
        error: 'Validación fallida',
        details: error.errors,
      });
    }

    res.status(400).json({
      error: error.message || 'Error al crear administrador',
    });
  }
};
