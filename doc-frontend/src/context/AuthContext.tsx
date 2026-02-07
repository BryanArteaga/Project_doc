import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { authService } from '../services/auth.service';
import type { Doctor, LoginCredentials, AuthContextType } from '../types';
import toast from 'react-hot-toast';
import { auditService, AuditAction, AuditLevel, logLogin, logLogout, logError } from '../utils/audit';
import { isValidJWT } from '../utils/security';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe ser usado dentro de AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [doctor, setDoctor] = useState<Doctor | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);

  // Verificar si hay sesión al cargar
  useEffect(() => {
    const initAuth = async () => {
      const savedToken = authService.getToken();
      const savedDoctor = authService.getDoctorData();

      if (savedToken && savedDoctor) {
        // Validar formato JWT
        if (!isValidJWT(savedToken)) {
          auditService.log(
            AuditAction.ERROR,
            AuditLevel.WARNING,
            'Token JWT inválido o expirado detectado al iniciar',
            { doctorId: savedDoctor.id }
          );
          authService.removeToken();
          setToken(null);
          setDoctor(null);
          setLoading(false);
          return;
        }

        setToken(savedToken);
        setDoctor(savedDoctor);
        
        // Verificar que el token siga siendo válido
        try {
          const { doctor: currentDoctor } = await authService.getCurrentDoctor();
          setDoctor(currentDoctor);
          authService.saveDoctorData(currentDoctor);
        } catch (error) {
          // Token inválido, limpiar
          logError(error as Error, 'Error verificando token en initAuth');
          authService.removeToken();
          setToken(null);
          setDoctor(null);
        }
      }
      
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = async (credentials: LoginCredentials) => {
    try {
      const response = await authService.login(credentials);
      
      // Validar token recibido
      if (!isValidJWT(response.token)) {
        auditService.log(
          AuditAction.ERROR,
          AuditLevel.CRITICAL,
          'Token JWT inválido recibido del servidor',
          { email: credentials.email }
        );
        throw new Error('Token inválido recibido del servidor');
      }

      authService.saveToken(response.token);
      authService.saveDoctorData(response.doctor);
      
      setToken(response.token);
      setDoctor(response.doctor);
      
      // Registrar login exitoso
      logLogin(`${response.doctor.nombre} ${response.doctor.apellido}`);
      
      toast.success(`Bienvenido ${response.doctor.nombre}!`);
      
      return response;
    } catch (error: any) {
      const message = error.response?.data?.error || 'Error al iniciar sesión';
      
      // Registrar intento fallido
      auditService.log(
        AuditAction.LOGIN,
        AuditLevel.WARNING,
        `Intento de login fallido: ${message}`,
        { email: credentials.email, error: message }
      );
      
      toast.error(message);
      throw error;
    }
  };

  const logout = async () => {
    const userName = doctor ? `${doctor.nombre} ${doctor.apellido}` : 'Usuario desconocido';
    
    try {
      setLoggingOut(true);
      await authService.logout();
      
      // Registrar logout exitoso
      logLogout(userName);
    } catch (error) {
      console.error('Error al cerrar sesión:', error);
      logError(error as Error, 'Error durante logout');
    } finally {
      authService.removeToken();
      setToken(null);
      setDoctor(null);
      setLoggingOut(false);
      toast.success('Sesión cerrada');
    }
  };

  const value: AuthContextType = {
    doctor,
    token,
    isAuthenticated: !!token && !!doctor,
    isAdmin: doctor?.rol === 'ADMIN',
    isDoctor: doctor?.rol === 'DOCTOR',
    login,
    logout,
    loading,
    loggingOut,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
