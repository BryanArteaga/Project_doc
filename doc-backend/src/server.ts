import express from 'express';
import cors from 'cors';
import compression from 'compression';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import dotenv from 'dotenv';
import authDoctorRoutes from './routes/auth.doctor.routes';
import usuarioRoutes from './routes/usuario.routes';
import registroSaludRoutes from './routes/registroSalud.routes';
import ejercicioRoutes from './routes/ejercicio.routes';
import asignacionEjercicioRoutes from './routes/asignacion-ejercicio.routes';
import consejoRoutes from './routes/consejo.routes';
import doctorRoutes from './routes/doctor.routes';
import syncRoutes from './routes/sync.routes';
import notificationRoutes from './routes/notification.routes';
import uploadRoutes from './routes/upload.routes';
import notaMedicaRoutes from './routes/notaMedica.routes';
import { prisma } from './lib/prisma';
import { initializeFirebase } from './config/firebase';
import { NotificationScheduler } from './services/notification-scheduler';

dotenv.config();

// ✅ Inicializar Firebase Admin SDK
initializeFirebase();

const app = express();
const PORT = process.env.PORT || 3000;

// Security Headers
app.use(helmet({
  contentSecurityPolicy: process.env.NODE_ENV === 'production' ? undefined : false
}));

// Rate limiting general
const generalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutos
  max: 500, // límite de 500 requests por IP (fue 100, permitir más para sincronización)
  message: { error: 'Demasiadas peticiones, intente más tarde' },
  standardHeaders: true,
  legacyHeaders: false,
  skip: (req) => {
    // Excluir endpoints de sync del rate limiting (son críticos para offline-first)
    return req.path.startsWith('/api/sync/') || req.path.startsWith('/api/usuarios/by-cedula');
  }
});

// Rate limiting estricto para rutas de autenticación
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutos
  max: 5, // máximo 5 intentos
  skipSuccessfulRequests: true, // no contar requests exitosos
  message: { error: 'Demasiados intentos de login, intente más tarde' },
  standardHeaders: true,
  legacyHeaders: false,
});

// CORS configuración
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization', 'x-doctor-token']
}));

// Compression para reducir ancho de banda
app.use(compression({
  filter: (req, res) => {
    if (req.headers['x-no-compression']) {
      return false;
    }
    return compression.filter(req, res);
  },
  level: 6 // Balance entre compresión y CPU
}));

app.use(express.json({ limit: '1mb' })); // Limitar tamaño de payloads
app.use(express.urlencoded({ extended: true, limit: '1mb' }));

// Aplicar rate limiting general a todas las rutas de API
app.use('/api/', generalLimiter);

// Health check endpoints (sin rate limiting)
app.get('/health', async (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    memory: {
      used: `${Math.round(process.memoryUsage().heapUsed / 1024 / 1024)}MB`,
      total: `${Math.round(process.memoryUsage().heapTotal / 1024 / 1024)}MB`
    }
  });
});

app.get('/health/db', async (req, res) => {
  try {
    await prisma.$queryRaw`SELECT 1`;
    res.json({ 
      status: 'ok', 
      database: 'connected',
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(503).json({ 
      status: 'error', 
      database: 'disconnected',
      timestamp: new Date().toISOString()
    });
  }
});

// Routes
app.get('/', (req, res) => {
  res.json({ 
    message: 'API Backend Doc - Sistema de Gestion de Pacientes',
    version: '1.0.0',
    health: '/health',
    docs: '/api-docs'
  });
});

// Auth routes con rate limiting estricto
app.use('/api/auth/doctor', authLimiter, authDoctorRoutes); // Doctores y Admins

// Resource routes
app.use('/api/usuarios', usuarioRoutes);
app.use('/api/registros-salud', registroSaludRoutes);
app.use('/api/ejercicios', ejercicioRoutes);
app.use('/api/asignaciones-ejercicios', asignacionEjercicioRoutes);
app.use('/api/consejos', consejoRoutes);
app.use('/api/doctor', doctorRoutes);
app.use('/api/sync', syncRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/upload', uploadRoutes);
app.use('/api/notas-medicas', notaMedicaRoutes);

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Error handler
app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error(err.stack);
  res.status(err.status || 500).json({
    error: err.message || 'Error interno del servidor'
  });
});

app.listen(PORT, () => {
  console.log(`Servidor corriendo en http://localhost:${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV}`);

  // ✅ Inicializar scheduler de notificaciones
  NotificationScheduler.start();
});

