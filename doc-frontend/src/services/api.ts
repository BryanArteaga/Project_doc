import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor para agregar el token
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('doctor_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: unknown) => {
    return Promise.reject(error);
  }
);

// Interceptor para manejar errores
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Token expirado o inválido
      // Solo redirigir si no es una petición al endpoint de login
      const isLoginRequest = error.config?.url?.includes('/login');
      
      if (!isLoginRequest) {
        localStorage.removeItem('doctor_token');
        localStorage.removeItem('doctor_data');
        // Usar location.replace para limpiar el historial
        setTimeout(() => {
          window.location.replace('/login');
        }, 100);
      }
    }
    return Promise.reject(error);
  }
);

export default api;
