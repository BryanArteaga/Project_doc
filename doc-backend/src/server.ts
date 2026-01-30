import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import authRoutes from './routes/auth.routes';
import usuarioRoutes from './routes/usuario.routes';
import registroSaludRoutes from './routes/registroSalud.routes';
import ejercicioRoutes from './routes/ejercicio.routes';
import doctorRoutes from './routes/doctor.routes';
dotenv.config();
const app = express();
const PORT = process.env.PORT || 3000;
// Middlewares
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
// Routes
app.use('/api/auth', authRoutes);
app.use('/api/usuarios', usuarioRoutes);
app.use('/api/registros-salud', registroSaludRoutes);
app.use('/api/ejercicios', ejercicioRoutes);
app.use('/api/doctor', doctorRoutes);
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
  console.log(`🚀 Servidor corriendo en http://localhost:${PORT}`);
  console.log(`📊 Environment: ${process.env.NODE_ENV}`);
});
