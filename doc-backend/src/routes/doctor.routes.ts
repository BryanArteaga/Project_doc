import { Router } from 'express';
import {
  getPatients,
  getPatient,
  getPendingPatients,
  activatePatient,
  deactivatePatient,
  assignPatientToDoctor,
} from '../controllers/patient.controller';
import { getDashboardStats, getAllDoctors, updateDoctor } from '../controllers/doctor.controller';
import { requireDoctor, requireDoctorAdmin } from '../middleware/auth.middleware';

const router = Router();

// Todas las rutas requieren autenticación de doctor
router.use(requireDoctor);

// Estadísticas
router.get('/stats', getDashboardStats);

// Rutas de gestión de doctores (solo admin)
router.get('/all', requireDoctorAdmin, getAllDoctors);
router.patch('/:id', requireDoctorAdmin, updateDoctor);

// Rutas de pacientes
router.get('/patients', getPatients);
router.get('/patients/pending', getPendingPatients);
router.get('/patients/:id', getPatient);
router.patch('/patients/:id/activate', activatePatient);
router.patch('/patients/:id/deactivate', deactivatePatient);
router.post('/patients/assign', assignPatientToDoctor);

export default router;
