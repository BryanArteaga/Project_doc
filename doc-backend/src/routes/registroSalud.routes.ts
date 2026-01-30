import { Router } from 'express';
import { 
  createRegistro, 
  getRegistros, 
  getRegistroById, 
  updateRegistro, 
  deleteRegistro 
} from '../controllers/registroSalud.controller';
import { authenticateToken } from '../middleware/auth.middleware';
const router = Router();
router.use(authenticateToken);
router.post('/', createRegistro);
router.get('/', getRegistros);
router.get('/:id', getRegistroById);
router.put('/:id', updateRegistro);
router.delete('/:id', deleteRegistro);
export default router;
