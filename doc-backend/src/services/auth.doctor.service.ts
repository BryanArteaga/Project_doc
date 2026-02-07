import { supabase, supabaseAdmin } from '../lib/supabase';
import { prisma } from '../lib/prisma';
import { StaffRole } from '../generated/prisma';

interface DoctorRegisterInput {
  email: string;
  password: string;
  nombre: string;
  apellido: string;
  cedula: string;
  telefono?: string;
  especialidad?: string;
  rol?: StaffRole;
}

interface DoctorLoginInput {
  email: string;
  password: string;
}

export class AuthDoctorService {
  /**
   * Registrar nuevo doctor (solo ADMIN puede registrar)
   */
  static async register(data: DoctorRegisterInput, adminId: string) {
    try {
      // Verificar que quien registra sea ADMIN
      const admin = await prisma.doctor.findUnique({
        where: { supabaseUserId: adminId }
      });

      if (!admin || admin.rol !== 'ADMIN') {
        throw new Error('No autorizado: Solo administradores pueden registrar doctores');
      }

      // Verificar si el email ya existe en nuestra BD
      const existingDoctor = await prisma.doctor.findUnique({
        where: { email: data.email }
      });

      if (existingDoctor) {
        throw new Error('El email ya está registrado');
      }

      // Verificar si la cédula ya existe
      const existingCedula = await prisma.doctor.findUnique({
        where: { cedula: data.cedula }
      });

      if (existingCedula) {
        throw new Error('La cédula ya está registrada');
      }

      // Crear usuario en Supabase Auth
      const { data: authData, error: authError } = await supabaseAdmin.auth.admin.createUser({
        email: data.email,
        password: data.password,
        email_confirm: true, // Auto-confirmar email
        user_metadata: {
          nombre: data.nombre,
          apellido: data.apellido,
          role: 'doctor'
        }
      });

      if (authError || !authData.user) {
        throw new Error(`Error al crear usuario en Supabase: ${authError?.message}`);
      }

      // Crear registro de doctor en nuestra BD
      const doctor = await prisma.doctor.create({
        data: {
          supabaseUserId: authData.user.id,
          email: data.email,
          nombre: data.nombre,
          apellido: data.apellido,
          cedula: data.cedula,
          telefono: data.telefono,
          especialidad: data.especialidad,
          rol: data.rol || 'DOCTOR',
          activo: true
        }
      });

      return {
        id: doctor.id,
        email: doctor.email,
        nombre: doctor.nombre,
        apellido: doctor.apellido,
        rol: doctor.rol,
        supabaseUserId: doctor.supabaseUserId
      };

    } catch (error: any) {
      console.error('Error en registro de doctor:', error);
      throw error;
    }
  }

  /**
   * Login de doctor
   */
  static async login(data: DoctorLoginInput) {
    try {
      // Autenticar con Supabase
      const { data: authData, error: authError } = await supabase.auth.signInWithPassword({
        email: data.email,
        password: data.password
      });

      if (authError || !authData.user || !authData.session) {
        throw new Error('Credenciales inválidas');
      }

      // Buscar doctor en nuestra BD
      const doctor = await prisma.doctor.findUnique({
        where: { supabaseUserId: authData.user.id }
      });

      if (!doctor) {
        throw new Error('Doctor no encontrado en el sistema');
      }

      if (!doctor.activo) {
        throw new Error('Cuenta desactivada. Contacte al administrador');
      }

      return {
        token: authData.session.access_token,
        refreshToken: authData.session.refresh_token,
        doctor: {
          id: doctor.id,
          email: doctor.email,
          nombre: doctor.nombre,
          apellido: doctor.apellido,
          rol: doctor.rol,
          especialidad: doctor.especialidad,
          supabaseUserId: doctor.supabaseUserId
        }
      };

    } catch (error: any) {
      console.error('Error en login de doctor:', error);
      throw error;
    }
  }

  /**
   * Obtener doctor por ID de Supabase
   */
  static async getDoctorBySupabaseId(supabaseUserId: string) {
    const doctor = await prisma.doctor.findUnique({
      where: { supabaseUserId }
    });

    if (!doctor) {
      throw new Error('Doctor no encontrado');
    }

    if (!doctor.activo) {
      throw new Error('Cuenta desactivada');
    }

    return {
      id: doctor.id,
      email: doctor.email,
      nombre: doctor.nombre,
      apellido: doctor.apellido,
      cedula: doctor.cedula,
      telefono: doctor.telefono,
      especialidad: doctor.especialidad,
      rol: doctor.rol,
      activo: doctor.activo,
      creadoEn: doctor.creadoEn,
      supabaseUserId: doctor.supabaseUserId
    };
  }

  /**
   * Logout (invalidar sesión en Supabase)
   */
  static async logout(token: string) {
    const { error } = await supabase.auth.admin.signOut(token);
    
    if (error) {
      console.error('Error al cerrar sesión:', error);
    }
    
    return { message: 'Sesión cerrada exitosamente' };
  }

  /**
   * Crear primer admin (solo si no existe ningún admin)
   */
  static async createFirstAdmin(data: DoctorRegisterInput) {
    try {
      // Verificar si ya existe algún admin
      const existingAdmin = await prisma.doctor.findFirst({
        where: { rol: 'ADMIN' }
      });

      if (existingAdmin) {
        throw new Error('Ya existe un administrador en el sistema');
      }

      // Verificar si el email ya existe
      const existingEmail = await prisma.doctor.findUnique({
        where: { email: data.email }
      });

      if (existingEmail) {
        throw new Error('El email ya está registrado');
      }

      // Crear usuario en Supabase Auth
      const { data: authData, error: authError } = await supabaseAdmin.auth.admin.createUser({
        email: data.email,
        password: data.password,
        email_confirm: true,
        user_metadata: {
          nombre: data.nombre,
          apellido: data.apellido,
          role: 'admin'
        }
      });

      if (authError || !authData.user) {
        throw new Error(`Error al crear usuario en Supabase: ${authError?.message}`);
      }

      // Crear registro de admin en nuestra BD
      const admin = await prisma.doctor.create({
        data: {
          supabaseUserId: authData.user.id,
          email: data.email,
          nombre: data.nombre,
          apellido: data.apellido,
          cedula: data.cedula,
          telefono: data.telefono,
          especialidad: data.especialidad,
          rol: 'ADMIN',
          activo: true
        }
      });

      return {
        id: admin.id,
        email: admin.email,
        nombre: admin.nombre,
        apellido: admin.apellido,
        rol: admin.rol,
        supabaseUserId: admin.supabaseUserId
      };

    } catch (error: any) {
      console.error('Error al crear primer admin:', error);
      throw error;
    }
  }
}
