import api from './api';
import type { Patient, DashboardStats } from '../types';

export const doctorService = {
  // Obtener estadísticas del dashboard
  async getStats(): Promise<DashboardStats> {
    const response = await api.get<DashboardStats>('/api/doctor/stats');
    return response.data;
  },

  // Obtener pacientes pendientes
  async getPendingPatients(): Promise<Patient[]> {
    const response = await api.get<{ patients: Patient[], pagination: any }>('/api/doctor/patients/pending');
    return response.data.patients || [];
  },

  // Obtener pacientes activos
  async getActivePatients(): Promise<Patient[]> {
    const response = await api.get<{ patients: Patient[], pagination: any }>('/api/doctor/patients');
    return response.data.patients || [];
  },

  // Activar paciente
  async activatePatient(patientId: number, doctorId: number): Promise<void> {
    await api.patch(`/api/doctor/patients/${patientId}/activate`, { doctorId });
  },

  // Desactivar paciente
  async deactivatePatient(patientId: number): Promise<void> {
    await api.patch(`/api/doctor/patients/${patientId}/deactivate`);
  },
};
