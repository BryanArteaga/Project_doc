import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import jwt from 'jsonwebtoken';
import { RegisterDTO, LoginDTO, AuthRequest } from '../types';

const prisma = new PrismaClient();

export const register = async (req: Request, res: Response) => {
  try {
    const data: RegisterDTO = req.body;

    // Validar que la cédula no exista
    const existingUser = await prisma.usuario.findUnique({
      where: { cedula: data.cedula }
    });

    if (existingUser) {
      return res.status(400).json({ error: 'La cédula ya está registrada' });
    }

    // Crear usuario con estado "pending" (pendiente de activación)
    const usuario = await prisma.usuario.create({
      data: {
        ...data,
        accountStatus: 'pending', // Estado inicial: pendiente
        notifDetails: '{}',
        registeredAt: new Date()
      }
    });

    // Generar token JWT
    const token = jwt.sign(
      { userId: usuario.id, cedula: usuario.cedula },
      process.env.JWT_SECRET || 'secreta123',
      { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
    );

    res.status(201).json({
      message: 'Usuario registrado exitosamente. Tu cuenta está pendiente de activación por el doctor.',
      usuario: {
        id: usuario.id,
        cedula: usuario.cedula,
        nombre: usuario.nombre,
        accountStatus: usuario.accountStatus,
        registeredAt: usuario.registeredAt
      },
      token
    });
  } catch (error) {
    console.error('Error en registro:', error);
    res.status(500).json({ error: 'Error al registrar usuario' });
  }
};

export const login = async (req: Request, res: Response) => {
  try {
    const { cedula }: LoginDTO = req.body;

    const usuario = await prisma.usuario.findUnique({
      where: { cedula }
    });

    if (!usuario) {
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }

    // Generar token JWT
    const token = jwt.sign(
      { userId: usuario.id, cedula: usuario.cedula },
      process.env.JWT_SECRET || 'secreta123',
      { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
    );

    // Actualizar última sincronización
    await prisma.usuario.update({
      where: { id: usuario.id },
      data: { lastSyncAt: new Date() }
    });

    res.json({
      message: 'Login exitoso',
      usuario: {
        id: usuario.id,
        cedula: usuario.cedula,
        nombre: usuario.nombre,
        edad: usuario.edad,
        peso: usuario.peso,
        altura: usuario.altura,
        sexo: usuario.sexo,
        enfermedad: usuario.enfermedad,
        colorEnfermedad: usuario.colorEnfermedad,
        accountStatus: usuario.accountStatus,
        activatedAt: usuario.activatedAt,
        notifEnabled: usuario.notifEnabled,
        notifType: usuario.notifType,
        notifDetails: usuario.notifDetails,
        glucosaInicial: usuario.glucosaInicial,
        colesterolInicial: usuario.colesterolInicial
      },
      token
    });
  } catch (error) {
    console.error('Error en login:', error);
    res.status(500).json({ error: 'Error al iniciar sesión' });
  }
};

export const checkAccountStatus = async (req: AuthRequest, res: Response) => {
  try {
    const usuario = await prisma.usuario.findUnique({
      where: { id: req.userId },
      select: {
        id: true,
        cedula: true,
        nombre: true,
        accountStatus: true,
        activatedAt: true,
        activatedBy: true
      }
    });

    if (!usuario) {
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }

    res.json(usuario);
  } catch (error) {
    console.error('Error al verificar estado:', error);
    res.status(500).json({ error: 'Error al verificar estado de cuenta' });
  }
};

