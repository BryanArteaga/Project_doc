import { Router } from 'express';
import {
  registerDoctor,
  loginDoctor,
  getCurrentDoctor,
  logoutDoctor,
  createFirstAdmin
} from '../controllers/auth.doctor.controller';
import { requireDoctorAdmin, requireDoctor } from '../middleware/auth.middleware';

const router = Router();

/**
 * @route   POST /api/auth/doctor/setup-admin
 * @desc    Crear primer administrador del sistema (solo si no existe)
 * @access  Public (pero solo funciona una vez)
 */
router.post('/setup-admin', createFirstAdmin);

/**
 * @route   POST /api/auth/doctor/login
 * @desc    Login de doctor
 * @access  Public
 */
router.post('/login', loginDoctor);

/**
 * @route   POST /api/auth/doctor/register
 * @desc    Registrar nuevo doctor (solo ADMIN)
 * @access  Private (Admin)
 */
router.post('/register', requireDoctorAdmin, registerDoctor);

/**
 * @route   GET /api/auth/doctor/me
 * @desc    Obtener información del doctor actual
 * @access  Private (Doctor)
 */
router.get('/me', requireDoctor, getCurrentDoctor);

/**
 * @route   POST /api/auth/doctor/logout
 * @desc    Cerrar sesión
 * @access  Private (Doctor)
 */
router.post('/logout', requireDoctor, logoutDoctor);

export default router;
