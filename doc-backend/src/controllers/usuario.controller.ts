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

    try {
      // � FIX: Verificar si la cédula YA EXISTE antes del upsert
      const usuarioExistente = cedula ? await prisma.usuario.findFirst({
        where: { cedula: cedula }
      }) : null;
      const usuarioYaExistia = !!usuarioExistente;

      // �🔧 UPSERT por cédula: crear si nuevo, actualizar solo token si existe
      // IMPORTANTE: No actualizar datos personales si el usuario ya existe
      // Solo admin puede cambiar información de pacientes existentes
      const usuario = await prisma.usuario.upsert({
        where: { cedula: cedula || null },
        update: {
          // ⚠️ SOLO actualizar token y timestamp si el usuario ya existe
          // NO actualizar nombre, apellido, enfermedad, colorEnfermedad, etc.
          // Esos datos solo pueden cambiar si es autorizado por admin
          tokenDispositivo: tokenDispositivo || undefined,
          ultimaSincronizacion: new Date(),
        },
        create: {
          // Crear NUEVO usuario con todos los datos
          nombre,
          apellido,
          cedula: cedula || null,
          fechaNacimiento: fechaNacimiento ? new Date(fechaNacimiento) : null,
          edad: edad || null,
          // ✅ Redondear peso a 1 decimal para evitar precisión flotante
          peso: peso ? Math.round(peso * 10) / 10 : null,
          altura: altura || null,
          sexo: sexo || null,
          enfermedad: enfermedad || null,
          colorEnfermedad: colorEnfermedad || null,
          tokenDispositivo: tokenDispositivo || null,
          estadoCuenta,
          idDoctorAsignado: defaultDoctorId,
          activadoEn: estadoCuenta === 'ACTIVE' ? new Date() : null,
          activadoPor: estadoCuenta === 'ACTIVE' ? defaultDoctorId : null,
          ultimaSincronizacion: new Date(),
        }
      });

      // 🔗 Si vino token, registrar dispositivo asociado al usuario
      if (tokenDispositivo) {
        try {
          await prisma.dispositivo.create({
            data: {
              usuarioId: usuario.id,
              fcmToken: tokenDispositivo,
              activo: true,
              ultimaVezVisto: new Date()
            }
          });
          console.log(`✅ Dispositivo FCM registrado para usuario ${usuario.id}`);
        } catch (deviceError: any) {
          // Si ya existe el token (único), actualizar asociación
          if (deviceError.code === 'P2002') {
            console.log(`ℹ️ Token FCM ya existía, actualizando asociación`);
            // Buscar dispositivo existente y actualizar usuarioId si es necesario
            try {
              await prisma.dispositivo.updateMany({
                where: { fcmToken: tokenDispositivo },
                data: { usuarioId: usuario.id, ultimaVezVisto: new Date() }
              });
            } catch (e: any) {
              console.log(`⚠️ Error actualizando dispositivo: ${e.message}`);
            }
          } else {
            console.log(`⚠️ Error al registrar dispositivo: ${deviceError.message}`);
          }
        }
      }

      // Determinar si fue creado (si la cédula NO existía antes)
      const wasCreated = !usuarioYaExistia;
      const statusCode = wasCreated ? 201 : 200;
      const mensaje = wasCreated 
        ? `Usuario creado: ${usuario.nombre} ${usuario.apellido} (ID: ${usuario.id})`
        : `Usuario existente recuperado: ${usuario.nombre} ${usuario.apellido} (ID: ${usuario.id}) - Datos protegidos`;
      
      console.log(`✅ ${mensaje} - Asignado a Doctor ID: ${defaultDoctorId}`);
      res.status(statusCode).json({
        message: mensaje,
        usuario,
        created: wasCreated,
        wasExistingUser: usuarioYaExistia
      });
    } catch (dbError: any) {
      // El upsert no debería generar P2002, pero si ocurre, manejar
      if (dbError.code === 'P2002') {
        return res.status(409).json({ 
          error: `Error de cédula única: ${cedula}`
        });
      }
      throw dbError;
    }
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
export const updateProfile = async (req: Request, res: Response) => {
  try {
    // Obtener usuarioId del body (sin JWT)
    const { 
      usuarioId,
      cedula,
      nombre,
      apellido,
      edad,
      peso, 
      altura,
      sexo,
      glucosaInicial,
      colesterolInicial,
      notifEnabled, 
      notifType, 
      notifDetails,
      confirmDataChange
    } = req.body;

    if (!usuarioId) {
      return res.status(400).json({ error: 'usuarioId requerido en body' });
    }

    // 🔍 OPCIÓN B: Verificar si el usuario YA EXISTÍA (por cédula)
    let isExistingUser = false;
    if (cedula) {
      const userByCedula = await prisma.usuario.findFirst({
        where: { cedula: cedula }
      });
      if (userByCedula && userByCedula.id !== usuarioId) {
        return res.status(409).json({ 
          error: 'La cédula ya está registrada a otro usuario' 
        });
      }
      if (userByCedula && userByCedula.id === usuarioId) {
        const now = new Date();
        const createdAt = new Date(userByCedula.creadoEn);
        const minutesSinceCreation = (now.getTime() - createdAt.getTime()) / (1000 * 60);
        isExistingUser = minutesSinceCreation > 2;
      }
    }

    // 🔍 Detectar si está intentando cambiar datos personales
    const isChangingPersonalData = nombre !== undefined || apellido !== undefined || 
                                    edad !== undefined || peso !== undefined || 
                                    altura !== undefined || sexo !== undefined;

    // ⚠️ OPCIÓN B: Si es usuario existente Y está cambiando datos personales
    // Requiere confirmación explícita
    if (isExistingUser && isChangingPersonalData && !confirmDataChange) {
      console.warn(`⚠️ Usuario existente (ID: ${usuarioId}) intenta cambiar datos sin confirmación`);
      const currentUser = await prisma.usuario.findUnique({ where: { id: usuarioId } });
      return res.status(403).json({
        error: 'Confirmación requerida para cambiar datos personales',
        requiresConfirmation: true,
        currentData: {
          nombre: currentUser?.nombre,
          apellido: currentUser?.apellido,
          edad: currentUser?.edad,
          peso: currentUser?.peso,
          altura: currentUser?.altura,
          sexo: currentUser?.sexo
        },
        proposedData: {
          nombre,
          apellido,
          edad,
          peso,
          altura,
          sexo
        },
        message: 'Mostrar diálogo de confirmación en app. Si usuario confirma, enviar nuevamente con confirmDataChange: true'
      });
    }

    const updateData: any = {};

    // ✅ Permitir cambiar datos personales si:
    // - No es usuario existente, O
    // - Es usuario existente PERO viene confirmDataChange: true
    if (!isExistingUser || confirmDataChange) {
      if (nombre !== undefined) updateData.nombre = nombre.trim();
      if (apellido !== undefined) updateData.apellido = apellido.trim();
      if (edad !== undefined) updateData.edad = parseInt(edad);
      if (peso !== undefined) updateData.peso = parseFloat(parseFloat(peso).toFixed(1));
      if (altura !== undefined) updateData.altura = parseFloat(parseFloat(altura).toFixed(2));
      if (sexo !== undefined) updateData.sexo = sexo.toUpperCase();
    }

    // 🔒 CEDULA NUNCA SE ACTUALIZA - Es un documento de identidad único e inmutable
    // Ignorar si viene en el request
    if (cedula !== undefined) {
      const currentUser = await prisma.usuario.findUnique({ where: { id: usuarioId } });
      if (currentUser && currentUser.cedula !== cedula) {
        console.warn(`⚠️ Intento de cambiar cédula bloqueado. ID: ${usuarioId}, Cédula anterior: ${currentUser.cedula}, Cédula propuesta: ${cedula}`);
      }
    }

    // ✅ SIEMPRE permitir: glucosa, colesterol, notificaciones
    if (glucosaInicial !== undefined) updateData.glucosaInicial = parseFloat(parseFloat(glucosaInicial).toFixed(1));
    if (colesterolInicial !== undefined) updateData.colesterolInicial = parseFloat(parseFloat(colesterolInicial).toFixed(1));
    if (notifEnabled !== undefined) updateData.notifEnabled = notifEnabled;
    if (notifType !== undefined) updateData.notifType = notifType;
    if (notifDetails !== undefined) updateData.notifDetails = notifDetails;

    const updatedUser = await prisma.usuario.update({
      where: { id: usuarioId },
      data: updateData
    });

    const successMessage = confirmDataChange && isExistingUser
      ? 'Datos personales actualizados con confirmación'
      : 'Perfil actualizado exitosamente';

    res.status(200).json({
      usuario: updatedUser,
      isExistingUser: isExistingUser,
      message: successMessage
    });
  } catch (error) {
    console.error('Error al actualizar perfil:', error);
    res.status(500).json({ error: 'Error al actualizar perfil' });
  }
};

/**
 * GET /api/usuarios/by-cedula/:cedula
 * Buscar usuario por cédula (para sincronización offline-first)
 * Usado cuando app se reinstala y necesita recuperar su cuenta por cédula
 */
export const searchByCedula = async (req: Request, res: Response) => {
  try {
    const { cedula } = req.params;

    if (!cedula || cedula.length !== 10) {
      return res.status(400).json({ 
        error: 'Cédula inválida o no proporcionada',
        found: false 
      });
    }

    const usuario = await prisma.usuario.findFirst({
      where: {
        cedula: cedula.trim(),
        // Buscar usuarios que NO estén suspendidos (puede ser ACTIVE o INACTIVE)
        estadoCuenta: {
          not: 'SUSPENDED'
        }
      },
      select: {
        id: true,
        cedula: true,
        nombre: true,
        apellido: true,
        edad: true,
        peso: true,
        altura: true,
        sexo: true,
        enfermedad: true,
        colorEnfermedad: true,
        estadoCuenta: true,
        registradoEn: true,
        idDoctorAsignado: true
      }
    });

    if (!usuario) {
      return res.status(404).json({ 
        error: 'Usuario no encontrado con esa cédula',
        found: false 
      });
    }

    res.json({
      success: true,
      found: true,
      usuario
    });
  } catch (error) {
    console.error('Error al buscar usuario por cédula:', error);
    res.status(500).json({ 
      error: 'Error al buscar usuario',
      found: false 
    });
  }
};
