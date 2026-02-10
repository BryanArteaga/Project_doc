import { Response, Request } from 'express';
import { prisma } from '../lib/prisma';
import { AuthRequest } from '../types';

/**
 * POST /api/usuarios/create
 * Crear usuario (DESARROLLO: sin autenticación)
 * Body: { 
 *   nombre: string, 
 *   apellido: string, 
 *   cedula?: string,
 *   fechaNacimiento?: string,  // Formato ISO (YYYY-MM-DD)
 *   edad?: number,
 *   peso?: number,
 *   altura?: number,
 *   sexo?: string,
 *   enfermedad?: string,
 *   colorEnfermedad?: string,
 *   tokenDispositivo?: string,
 *   estadoCuenta?: "ACTIVE" | "INACTIVE" 
 * }
 * 
 * Todos los usuarios son asignados automáticamente al doctor por defecto (ID del env o 2)
 */
export const createUsuario = async (req: Request, res: Response) => {
  try {
    const { 
      nombre, 
      apellido, 
      cedula,
      fechaNacimiento,
      edad,
      peso,
      altura,
      sexo,
      enfermedad,
      colorEnfermedad,
      tokenDispositivo,
      estadoCuenta = 'ACTIVE' 
    } = req.body;
    
    if (!nombre || !apellido) {
      return res.status(400).json({ error: 'nombre y apellido requeridos' });
    }

    // Obtener ID del doctor por defecto de variable de entorno o usar 2
    const defaultDoctorId = parseInt(process.env.DEFAULT_DOCTOR_ID || '2');

    const usuario = await prisma.usuario.create({
      data: {
        nombre,
        apellido,
        cedula: cedula || null,
        fechaNacimiento: fechaNacimiento ? new Date(fechaNacimiento) : null,
        edad: edad || null,
        peso: peso || null,
        altura: altura || null,
        sexo: sexo || null,
        enfermedad: enfermedad || null,
        colorEnfermedad: colorEnfermedad || null,
        tokenDispositivo: tokenDispositivo || null,
        estadoCuenta,
        idDoctorAsignado: defaultDoctorId,  // Asignar automáticamente al doctor por defecto
        // Si estadoCuenta es ACTIVE, marcarlo como activado automáticamente
        activadoEn: estadoCuenta === 'ACTIVE' ? new Date() : null,
        activadoPor: estadoCuenta === 'ACTIVE' ? defaultDoctorId : null,
        ultimaSincronizacion: new Date(),
      }
    });

    console.log(`✅ Usuario creado: ${usuario.nombre} ${usuario.apellido} (ID: ${usuario.id}) - Asignado a Doctor ID: ${defaultDoctorId}`);
    res.status(201).json({
      message: 'Usuario creado exitosamente',
      usuario
    });
  } catch (error: any) {
    console.error('Error al crear usuario:', error);
    res.status(500).json({ error: 'Error al crear usuario', details: error.message });
  }
};

export const getProfile = async (req: AuthRequest, res: Response) => {
  try {
    const usuario = await prisma.usuario.findUnique({
      where: { id: req.userId }
    });
    if (!usuario) {
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }
    res.json(usuario);
  } catch (error) {
    console.error('Error al obtener perfil:', error);
    res.status(500).json({ error: 'Error al obtener perfil' });
  }
};
export const updateProfile = async (req: AuthRequest, res: Response) => {
  try {
    const { peso, notifEnabled, notifType, notifDetails } = req.body;
    const updatedUser = await prisma.usuario.update({
      where: { id: req.userId },
      data: {
        ...(peso !== undefined && { peso }),
        ...(notifEnabled !== undefined && { notifEnabled }),
        ...(notifType !== undefined && { notifType }),
        ...(notifDetails !== undefined && { notifDetails })
      }
    });
    res.json({
      message: 'Perfil actualizado exitosamente',
      usuario: updatedUser
    });
  } catch (error) {
    console.error('Error al actualizar perfil:', error);
    res.status(500).json({ error: 'Error al actualizar perfil' });
  }
};
