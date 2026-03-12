import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { doctorService } from '../../services/doctor.service';
import type { DashboardStats } from '../../types';
import { Users, TrendingUp, Dumbbell, Lightbulb } from 'lucide-react';
import toast from 'react-hot-toast';
import Layout from '../../components/common/Layout/Layout';
import Loading from '../../components/common/Loading';
import PageTransition from '../../components/common/PageTransition';
import './DoctorDashboard.css';

const DoctorDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { doctor, isAuthenticated } = useAuth();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }
    loadData();
  }, [isAuthenticated]);

  const loadData = async () => {
    try {
      setLoading(true);
      const statsData = await doctorService.getStats();
      setStats(statsData);
    } catch (error: any) {
      console.error('Error al cargar datos:', error);
      if (error.response?.status === 401) {
        toast.error('Sesión expirada. Redirigiendo al login...');
      } else {
        toast.error('Error al cargar los datos');
      }
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Layout>
        <Loading message="Cargando datos..." fullHeight />
      </Layout>
    );
  }

  return (
    <Layout>
      <PageTransition>
        <div className="dashboard-container">
          {/* Header Section */}
          <div className="dashboard-header">
            <h1 className="dashboard-title">Dashboard - Doctor</h1>
            <p className="dashboard-subtitle">
              Vista general de tus pacientes y actividad
            </p>
          </div>

        {/* Stats Cards */}
        <div className="stats-grid">
          <StatCard
            title="Total Pacientes"
            value={stats?.totalPacientes || 0}
            icon={<Users className="w-6 h-6" />}
            color="bg-blue-700"
          />
          <StatCard
            title="Registros Hoy"
            value={stats?.registrosHoy || 0}
            icon={<TrendingUp className="w-6 h-6" />}
            color="bg-purple-700"
          />
        </div>

        {/* Acciones Rápidas */}
        <div className="actions-section">
          <h2 className="section-title">Acciones Rápidas</h2>
          <div className="actions-grid">
            <ActionCard
              title="Gestionar Pacientes"
              description="Ver y administrar todos tus pacientes"
              icon={<Users className="w-6 h-6" />}
              onClick={() => navigate('/dashboard/patients')}
            />
            <ActionCard
              title="Asignar Ejercicios"
              description="Crear y gestionar asignaciones de ejercicios"
              icon={<Dumbbell className="w-6 h-6" />}
              onClick={() => navigate('/dashboard/asignaciones-ejercicios')}
            />
            <ActionCard
              title="Gestión de Ejercicios"
              description="Crear, editar y eliminar ejercicios del catálogo"
              icon={<Dumbbell className="w-6 h-6" />}
              onClick={() => navigate('/dashboard/gestion-ejercicios')}
            />
            <ActionCard
              title="Añadir Consejos"
              description="Próximamente disponible"
              icon={<Lightbulb className="w-6 h-6" />}
              disabled
            />
          </div>
        </div>
        </div>
      </PageTransition>
    </Layout>
  );
};

// Componente de tarjeta de acción
interface ActionCardProps {
  title: string;
  description: string;
  icon: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
}

const ActionCard: React.FC<ActionCardProps> = ({ title, description, icon, onClick, disabled }) => {
  return (
    <div 
      className={`action-card ${disabled ? 'action-card-disabled' : ''}`}
      onClick={!disabled ? onClick : undefined}
    >
      <div className="action-card-icon">{icon}</div>
      <div className="action-card-content">
        <h3 className="action-card-title">{title}</h3>
        <p className="action-card-description">{description}</p>
      </div>
      {disabled && <span className="action-card-badge">Próximamente</span>}
    </div>
  );
};

// Componente de tarjeta de estadísticas
interface StatCardProps {
  title: string;
  value: number;
  icon: React.ReactNode;
  color: string;
}

const StatCard: React.FC<StatCardProps> = ({ title, value, icon, color }) => {
  const colorMap: { [key: string]: string } = {
    'bg-blue-700': '#1d4ed8',
    'bg-green-700': '#15803d',
    'bg-yellow-700': '#a16207',
    'bg-purple-700': '#7e22ce'
  };

  return (
    <div className="stat-card">
      <div className="stat-card-content">
        <div className="stat-card-info">
          <h3>{title}</h3>
          <p>{value}</p>
        </div>
        <div 
          className="stat-card-icon"
          style={{ backgroundColor: colorMap[color] || '#5B65DC' }}
        >
          {icon}
        </div>
      </div>
    </div>
  );
};

export default DoctorDashboard;
