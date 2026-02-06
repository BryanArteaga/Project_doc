import jwt, { SignOptions } from 'jsonwebtoken';
import bcrypt from 'bcryptjs';
import { prisma } from '../lib/prisma';
import { AccountStatus } from '../types/prisma';

interface RegisterInput {
  email: string;
  password: string;
  nombre: string;
  apellido: string;
  telefonoPersonal?: string;
  fechaNacimiento?: string;
  sexo?: string;
}

interface LoginInput {
  email: string;
  password: string;
}

interface TokenPayload {
  usuarioId: number;
  email: string;
  estadoCuenta: AccountStatus;
}

export class AuthService {
  /**
   * Hashear contraseña
   */
  static async hashPassword(password: string): Promise<string> {
    const saltRounds = 10;
    return bcrypt.hash(password, saltRounds);
  }

  /**
   * Comparar contraseña
   */
  static async comparePassword(password: string, hash: string): Promise<boolean> {
    return bcrypt.compare(password, hash);
  }

  /**
   * Generar JWT token
   */
  static generateToken(payload: TokenPayload): string {
    const secret = process.env.JWT_SECRET || 'secreta123';
    const expiresIn = process.env.JWT_EXPIRES_IN || '7d';
    
    return jwt.sign(payload, secret as string, { expiresIn } as SignOptions);
  }

  /**
   * Verificar JWT token
   */
  static verifyToken(token: string): TokenPayload | null {
    try {
      const secret = process.env.JWT_SECRET || 'secreta123';
      return jwt.verify(token, secret) as TokenPayload;
    } catch (error) {
      return null;
    }
  }

  /**
   * Registrar nuevo usuario
   */
  static async register(input: RegisterInput) {
    // Validar que el email no exista
    const existingUser = await prisma.usuario.findUnique({
      where: { email: input.email },
    });

    if (existingUser) {
      throw new Error('El email ya está registrado');
    }

    // Hashear contraseña
    const hashedPassword = await this.hashPassword(input.password);

    // Crear usuario (paciente)
    const user = await prisma.usuario.create({
      data: {
        email: input.email,
        password: hashedPassword,
        nombre: input.nombre,
        apellido: input.apellido,
        telefonoPersonal: input.telefonoPersonal,
        estadoCuenta: AccountStatus.INACTIVE,
        fechaNacimiento: input.fechaNacimiento ? new Date(input.fechaNacimiento) : null,
        sexo: input.sexo,
      },
    });

    return {
      id: user.id,
      email: user.email,
      nombre: user.nombre,
      apellido: user.apellido,
      estadoCuenta: user.estadoCuenta,
    };
  }

  /**
   * Login de usuario
   */
  static async login(input: LoginInput) {
    // Buscar usuario
    const user = await prisma.usuario.findUnique({
      where: { email: input.email },
    });

    if (!user) {
      throw new Error('Email o contraseña incorrectos');
    }

    // Validar contraseña
    const passwordValid = await this.comparePassword(input.password, user.password);
    if (!passwordValid) {
      throw new Error('Email o contraseña incorrectos');
    }

    // Generar token
    const token = this.generateToken({
      usuarioId: user.id,
      email: user.email,
      estadoCuenta: user.estadoCuenta,
    });

    return {
      token,
      user: {
        id: user.id,
        email: user.email,
        nombre: user.nombre,
        apellido: user.apellido,
        estadoCuenta: user.estadoCuenta,
        assignedDoctorId: user.idDoctorAsignado,
      },
    };
  }

  /**
   * Obtener usuario por ID
   */
  static async getUserById(userId: number) {
    const user = await prisma.usuario.findUnique({
      where: { id: userId },
      select: {
        id: true,
        email: true,
        nombre: true,
        apellido: true,
        telefonoPersonal: true,
        estadoCuenta: true,
        fechaNacimiento: true,
        sexo: true,
        idDoctorAsignado: true,
        registradoEn: true,
        activadoEn: true,
      },
    });

    if (!user) {
      throw new Error('Usuario no encontrado');
    }

    return user;
  }

  /**
   * Actualizar dispositivo token (para notificaciones push)
   */
  static async updateDeviceToken(userId: number, tokenDispositivo: string) {
    return prisma.usuario.update({
      where: { id: userId },
      data: { tokenDispositivo },
    });
  }
}
