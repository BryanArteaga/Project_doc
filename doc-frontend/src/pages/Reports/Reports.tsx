import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../../components/common/Layout/Layout';
import { ArrowLeft, TrendingUp, Users, Activity, Calendar } from 'lucide-react';
import './Reports.css';

const Reports: React.FC = () => {
  const navigate = useNavigate();
  const [dateRange, setDateRange] = useState('month');

  const stats = [
    { label: 'Total Registros', value: '0', icon: <Activity />, color: 'blue' },
    { label: 'Pacientes Activos', value: '0', icon: <Users />, color: 'green' },
    { label: 'Promedio Diario', value: '0', icon: <TrendingUp />, color: 'purple' },
    { label: 'Último Mes', value: '0', icon: <Calendar />, color: 'orange' }
  ];

  return (
    <Layout>
      <div className="reports-container">
        <div className="reports-header">
          <button
            onClick={() => navigate('/admin')}
            style={{
              backgroundColor: '#5B65DC',
              color: '#ffffff',
              border: 'none',
              padding: '0.5rem 1rem',
              borderRadius: '0.5rem',
              fontWeight: 600,
              cursor: 'pointer',
              fontSize: '1rem',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              transition: 'all 0.3s ease',
              boxShadow: '0 4px 20px rgba(91, 101, 220, 0.15)'
            }}
            onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#4A52C4'}
            onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#5B65DC'}
          >
            <ArrowLeft style={{ width: '1rem', height: '1rem' }} />
            Volver
          </button>
          <h1 className="reports-title">Reportes y Estadísticas</h1>
          <p className="reports-subtitle">Análisis detallado del sistema</p>
        </div>

        {/* Filtros de fecha */}
        <div className="reports-filters">
          <div className="filter-buttons">
            <button
              className={`filter-btn ${dateRange === 'week' ? 'active' : ''}`}
              onClick={() => setDateRange('week')}
            >
              Esta Semana
            </button>
            <button
              className={`filter-btn ${dateRange === 'month' ? 'active' : ''}`}
              onClick={() => setDateRange('month')}
            >
              Este Mes
            </button>
            <button
              className={`filter-btn ${dateRange === 'year' ? 'active' : ''}`}
              onClick={() => setDateRange('year')}
            >
              Este Año
            </button>
          </div>
        </div>

        {/* Stats Grid */}
        <div className="reports-stats-grid">
          {stats.map((stat, index) => (
            <div key={index} className="reports-stat-card">
              <div className={`stat-icon ${stat.color}`}>
                {stat.icon}
              </div>
              <div className="stat-content">
                <p className="stat-label">{stat.label}</p>
                <p className="stat-value">{stat.value}</p>
              </div>
            </div>
          ))}
        </div>

        {/* Gráficos placeholder */}
        <div className="reports-charts">
          <div className="chart-card">
            <h3 className="chart-title">Registros de Salud por Mes</h3>
            <div className="chart-placeholder">
              <Activity className="chart-placeholder-icon" />
              <p>Gráfico de registros próximamente</p>
            </div>
          </div>

          <div className="chart-card">
            <h3 className="chart-title">Actividad de Pacientes</h3>
            <div className="chart-placeholder">
              <Users className="chart-placeholder-icon" />
              <p>Gráfico de actividad próximamente</p>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default Reports;
