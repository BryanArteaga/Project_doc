import { Router } from 'express';
import {
  getPatients,
  getPatient,
  getPendingPatients,
  activatePatient,
  deactivatePatient,
  assignPatientToDoctor,
} from '../controllers/patient.controller';
import { requireAuth } from '../middleware/auth.middleware';

const router = Router();

// Todas las rutas requieren autenticación (TODO: agregar autenticación específica de doctor)
router.use(...requireAuth);

// Rutas de pacientes
router.get('/patients', getPatients);
router.get('/patients/pending', getPendingPatients);
router.get('/patients/:id', getPatient);
router.patch('/patients/:id/activate', activatePatient);
router.patch('/patients/:id/deactivate', deactivatePatient);
router.post('/patients/assign', assignPatientToDoctor);

export default router;
