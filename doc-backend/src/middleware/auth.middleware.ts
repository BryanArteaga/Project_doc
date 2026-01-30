import { Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { AuthRequest } from '../types';
export const authenticateToken = (req: AuthRequest, res: Response, next: NextFunction) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  if (!token) {
    return res.status(401).json({ error: 'Token no proporcionado' });
  }
  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET || 'secreta123') as { 
      userId: number; 
      cedula: string 
    };
    req.userId = decoded.userId;
    req.userCedula = decoded.cedula;
    next();
  } catch (error) {
    return res.status(403).json({ error: 'Token inválido o expirado' });
  }
};
export const authenticateDoctor = (req: AuthRequest, res: Response, next: NextFunction) => {
  // Por ahora, el doctor usa una cédula específica o un token especial
  // Esto puede mejorarse con un modelo Doctor separado
  const doctorToken = req.headers['x-doctor-token'];
  if (!doctorToken || doctorToken !== process.env.DOCTOR_TOKEN) {
    return res.status(403).json({ error: 'Acceso denegado: se requiere autorización de doctor' });
  }
  next();
};
