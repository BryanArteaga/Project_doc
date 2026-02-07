import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { doctorService } from '../../services/doctor.service';
import type { DashboardStats } from '../../types';
import { 
  Users, 
  Activity, 
  UserCheck, 
  Clock, 
  TrendingUp,
  UserPlus,
  BarChart3,
  FileText
} from 'lucide-react';
import toast from 'react-hot-toast';
import Layout from '../../components/common/Layout/Layout';
import Loading from '../../components/common/Loading';
import PageTransition from '../../components/common/PageTransition';
import './AdminDashboard.css';

const AdminDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
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
    } catch (error) {
      console.error('Error al cargar datos:', error);
      toast.error('Error al cargar los datos');
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
          {/* Header */}
          <div className="dashboard-header">
          <h1 className="dashboard-title">Panel de Administración</h1>
          <p className="dashboard-subtitle">
            Vista general del sistema y gestión de usuarios
          </p>
        </div>

        {/* Stats Cards */}
        <div className="stats-grid">
          <StatsCard
            title="Total Pacientes"
            value={stats?.totalPacientes || 0}
            subtitle="En el sistema"
            icon={<Users className="w-8 h-8" />}
            gradient="from-blue-500 to-blue-600"
            textColor="text-blue-600"
            bgColor="bg-blue-50"
          />
          <StatsCard
            title="Pacientes Activos"
            value={stats?.pacientesActivos || 0}
            subtitle="Con cuenta activa"
            icon={<UserCheck className="w-8 h-8" />}
            gradient="from-green-500 to-green-600"
            textColor="text-green-600"
            bgColor="bg-green-50"
          />
          <StatsCard
            title="Pendientes"
            value={stats?.pacientesPendientes || 0}
            subtitle="Esperando activación"
            icon={<Clock className="w-8 h-8" />}
            gradient="from-yellow-500 to-yellow-600"
            textColor="text-yellow-600"
            bgColor="bg-yellow-50"
          />
          <StatsCard
            title="Registros Hoy"
            value={stats?.registrosHoy || 0}
            subtitle="Registros de salud"
            icon={<TrendingUp className="w-8 h-8" />}
            gradient="from-purple-500 to-purple-600"
            textColor="text-purple-600"
            bgColor="bg-purple-50"
          />
        </div>

        {/* Acciones Rápidas */}
        <div className="actions-grid">
          <ActionCard
            title="Registrar Doctor"
            description="Agregar nuevo doctor al sistema"
            icon={<UserPlus className="w-6 h-6" />}
            color="bg-indigo-700"
            onClick={() => navigate('/admin/register-doctor')}
          />
          <ActionCard
            title="Gestionar Doctores"
            description="Editar doctores y permisos"
            icon={<UserCheck className="w-6 h-6" />}
            color="bg-blue-700"
            onClick={() => navigate('/admin/manage-doctors')}
          />
          <ActionCard
            title="Ver Reportes"
            description="Estadísticas y reportes detallados"
            icon={<BarChart3 className="w-6 h-6" />}
            color="bg-green-700"
            onClick={() => navigate('/admin/reports')}
          />
          <ActionCard
            title="Gestionar Pacientes"
            description="Administrar todos los pacientes"
            icon={<Users className="w-6 h-6" />}
            color="bg-purple-700"
            onClick={() => navigate('/admin/manage-patients')}
          />
          <ActionCard
            title="Logs de Auditoría"
            description="Ver historial y eventos del sistema"
            icon={<FileText className="w-6 h-6" />}
            color="bg-indigo-700"
            onClick={() => navigate('/admin/audit-logs')}
          />
        </div>

        {/* Panel de Información */}
        <div className="info-grid">
          {/* Actividad Reciente */}
          <div className="info-panel">
            <div className="info-panel-header">
              <h2 className="info-panel-title">Actividad Reciente</h2>
            </div>
            <div className="info-panel-content">
              <div className="empty-state">
                <Activity className="empty-icon" />
                <p className="empty-text">No hay actividad reciente</p>
                <p className="empty-subtext">La actividad del sistema aparecerá aquí</p>
              </div>
            </div>
          </div>

          {/* Resumen del Sistema */}
          <div className="info-panel">
            <div className="info-panel-header">
              <h2 className="info-panel-title">Resumen del Sistema</h2>
            </div>
            <div className="info-panel-content">
              <SummaryItem
                label="Tasa de Activación"
                value={`${stats?.pacientesActivos || 0}/${stats?.totalPacientes || 0}`}
                percentage={
                  stats?.totalPacientes
                    ? Math.round(((stats?.pacientesActivos || 0) / stats.totalPacientes) * 100)
                    : 0
                }
              />
              <SummaryItem
                label="Pacientes Inactivos"
                value={`${stats?.pacientesInactivos || 0}`}
              />
              <SummaryItem
                label="Registros Promedio Diario"
                value={`${stats?.registrosHoy || 0}`}
              />
            </div>
          </div>
        </div>
      </div>
      </PageTransition>
    </Layout>
  );
};

// Componente de tarjeta de estadísticas
interface StatsCardProps {
  title: string;
  value: number;
  subtitle: string;
  icon: React.ReactNode;
  gradient: string;
  textColor: string;
  bgColor: string;
}

const StatsCard: React.FC<StatsCardProps> = ({ 
  title, 
  value, 
  subtitle, 
  icon, 
  gradient,
  textColor,
  bgColor 
}) => {
  return (
    <div className="stats-card">
      <div className={`stats-card-header bg-gradient-to-r ${gradient}`} />
      <div className="stats-card-content">
        <div className="stats-card-icon-wrapper">
          <div className={`stats-card-icon ${bgColor} ${textColor}`}>
            {icon}
          </div>
        </div>
        <p className="stats-card-title">{title}</p>
        <p className="stats-card-value">{value}</p>
        <p className="stats-card-subtitle">{subtitle}</p>
      </div>
    </div>
  );
};

// Componente de tarjeta de acción
interface ActionCardProps {
  title: string;
  description: string;
  icon: React.ReactNode;
  color: string;
  onClick: () => void;
}

const ActionCard: React.FC<ActionCardProps> = ({ 
  title, 
  description, 
  icon, 
  color,
  onClick 
}) => {
  // Mapear colores de Tailwind a colores hex
  const colorMap: { [key: string]: string } = {
    'bg-indigo-700': '#4338ca',
    'bg-blue-700': '#1d4ed8',
    'bg-green-700': '#15803d',
    'bg-purple-700': '#7e22ce'
  };

  return (
    <button onClick={onClick} className="action-card">
      <div 
        className="action-card-icon"
        style={{ backgroundColor: colorMap[color] || '#5B65DC' }}
      >
        {icon}
      </div>
      <h3 className="action-card-title">{title}</h3>
      <p className="action-card-description">{description}</p>
    </button>
  );
};

// Componente de item de resumen
interface SummaryItemProps {
  label: string;
  value: string;
  percentage?: number;
}

const SummaryItem: React.FC<SummaryItemProps> = ({ 
  label, 
  value, 
  percentage
}) => {
  return (
    <div className="summary-item">
      <span className="summary-label">{label}</span>
      <div className="summary-value-wrapper">
        <span className="summary-value">{value}</span>
        {percentage !== undefined && (
          <span className="summary-percentage">({percentage}%)</span>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;
