import api from './api';
import type { LoginCredentials, LoginResponse, Doctor } from '../types';

export const authService = {
  // Login
  async login(credentials: LoginCredentials): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/api/auth/doctor/login', credentials);
    return response.data;
  },

  // Register doctor
  async register(data: any): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/api/auth/doctor/register', data);
    return response.data;
  },

  // Obtener info del doctor actual
  async getCurrentDoctor(): Promise<{ doctor: Doctor }> {
    const response = await api.get<{ doctor: Doctor }>('/api/auth/doctor/me');
    return response.data;
  },

  // Logout
  async logout(): Promise<void> {
    await api.post('/api/auth/doctor/logout');
  },

  // Guardar token
  saveToken(token: string): void {
    localStorage.setItem('doctor_token', token);
  },

  // Obtener token
  getToken(): string | null {
    return localStorage.getItem('doctor_token');
  },

  // Eliminar token
  removeToken(): void {
    localStorage.removeItem('doctor_token');
    localStorage.removeItem('doctor_data');
  },

  // Guardar datos del doctor
  saveDoctorData(doctor: Doctor): void {
    localStorage.setItem('doctor_data', JSON.stringify(doctor));
  },

  // Obtener datos del doctor
  getDoctorData(): Doctor | null {
    const data = localStorage.getItem('doctor_data');
    return data ? JSON.parse(data) : null;
  },
};
