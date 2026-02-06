import { Request, Response } from 'express';
import { AuthService } from '../services/auth.service';
import { z } from 'zod';

// Schemas de validación (solo pacientes se registran via app)
const RegisterSchema = z.object({
  email: z.string().email('Email inválido'),
  password: z.string().min(6, 'Password debe tener al menos 6 caracteres'),
  nombre: z.string().min(2, 'Nombre debe tener al menos 2 caracteres'),
  apellido: z.string().min(2, 'Apellido debe tener al menos 2 caracteres'),
  telefonoPersonal: z.string().optional(),
  fechaNacimiento: z.string().optional(),
  sexo: z.string().optional(),
});

const LoginSchema = z.object({
  email: z.string().email('Email inválido'),
  password: z.string().min(6, 'Password debe tener al menos 6 caracteres'),
});

const UpdateDeviceTokenSchema = z.object({
  tokenDispositivo: z.string().min(10, 'Token inválido'),
});

/**
 * Registrar nuevo usuario
 */
export const register = async (req: Request, res: Response) => {
  try {
    const data = RegisterSchema.parse(req.body);
    const user = await AuthService.register(data);

    res.status(201).json({
      message: 'Usuario registrado exitosamente',
      user,
    });
  } catch (error: any) {
    console.error('Error en registro:', error);

    if (error instanceof z.ZodError) {
      return res.status(400).json({
        error: 'Validación fallida',
        details: error.errors,
      });
    }

    res.status(400).json({
      error: error.mensaje || 'Error al registrar usuario',
    });
  }
};

/**
 * Login de usuario
 */
export const login = async (req: Request, res: Response) => {
  try {
    const data = LoginSchema.parse(req.body);
    const result = await AuthService.login(data);

    res.json({
      message: 'Login exitoso',
      token: result.token,
      user: result.user,
    });
  } catch (error: any) {
    console.error('Error en login:', error);

    if (error instanceof z.ZodError) {
      return res.status(400).json({
        error: 'Validación fallida',
        details: error.errors,
      });
    }

    res.status(400).json({
      error: error.mensaje || 'Error al iniciar sesión',
    });
  }
};

/**
 * Obtener perfil del usuario actual
 */
export const me = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const user = await AuthService.getUserById(req.userId);
    res.json(user);
  } catch (error: any) {
    console.error('Error al obtener perfil:', error);
    res.status(404).json({
      error: error.mensaje || 'Usuario no encontrado',
    });
  }
};

/**
 * Actualizar device token para notificaciones push
 */
export const updateDeviceToken = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const data = UpdateDeviceTokenSchema.parse(req.body);
    await AuthService.updateDeviceToken(req.userId, data.tokenDispositivo);

    res.json({ message: 'Device token actualizado' });
  } catch (error: any) {
    console.error('Error al actualizar device token:', error);

    if (error instanceof z.ZodError) {
      return res.status(400).json({
        error: 'Validación fallida',
        details: error.errors,
      });
    }

    res.status(400).json({
      error: error.mensaje || 'Error al actualizar device token',
    });
  }
};


