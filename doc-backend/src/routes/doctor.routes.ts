import { Router } from 'express';
import { 
  getPendingUsers, 
  getActiveUsers, 
  activateUser, 
  deactivateUser,
  getDashboardStats,
  getUserDetails
} from '../controllers/doctor.controller';
import { authenticateDoctor } from '../middleware/auth.middleware';
const router = Router();
router.use(authenticateDoctor);
router.get('/stats', getDashboardStats);
router.get('/users/pending', getPendingUsers);
router.get('/users/active', getActiveUsers);
router.get('/users/:userId', getUserDetails);
router.post('/users/:userId/activate', activateUser);
router.post('/users/:userId/deactivate', deactivateUser);
export default router;
