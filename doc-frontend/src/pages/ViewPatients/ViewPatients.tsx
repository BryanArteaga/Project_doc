import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../../components/common/Layout/Layout';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import PageTransition from '../../components/common/PageTransition';
import { ArrowLeft, Search, Eye, FileText } from 'lucide-react';
import toast from 'react-hot-toast';
import { doctorService } from '../../services/doctor.service';
import type { Patient } from '../../types';
import './ViewPatients.css';

const ViewPatients: React.FC = () => {
  const navigate = useNavigate();
  const [patients, setPatients] = useState<Patient[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('all');

  useEffect(() => {
    loadPatients();
  }, []);

  const loadPatients = async () => {
    try {
      setLoading(true);
      const data = await doctorService.getActivePatients();
      setPatients(data);
    } catch (error: any) {
      console.error('Error al cargar pacientes:', error);
      toast.error('Error al cargar pacientes');
    } finally {
      setLoading(false);
    }
  };

  const filteredPatients = patients.filter(patient => {
    const matchesSearch = 
      patient.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
      patient.apellido.toLowerCase().includes(searchTerm.toLowerCase()) ||
      patient.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (patient.enfermedad && patient.enfermedad.toLowerCase().includes(searchTerm.toLowerCase()));
    
    const matchesStatus = 
      filterStatus === 'all' || 
      patient.estadoCuenta === filterStatus;

    return matchesSearch && matchesStatus;
  });

  if (loading) {
    return (
      <Layout>
        <Loading message="Cargando pacientes..." fullHeight />
      </Layout>
    );
  }

  return (
    <Layout>
      <PageTransition>
        <div className="view-patients-container">
          <div className="view-patients-header">
            <button
              onClick={() => navigate('/dashboard')}
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
            <h1 className="view-patients-title">Mis Pacientes</h1>
            <p className="view-patients-subtitle">
              Total: {filteredPatients.length} paciente(s)
            </p>
          </div>

        {/* Filtros */}
        <div className="filters-section">
          <div className="search-container">
            <Search className="search-icon" />
            <input
              type="text"
              placeholder="Buscar por nombre, email o enfermedad..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
          </div>
          
          <div className="status-filter">
            <label className="filter-label">Estado:</label>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="filter-select"
            >
              <option value="all">Todos</option>
              <option value="ACTIVE">Activos</option>
              <option value="INACTIVE">Inactivos</option>
              <option value="SUSPENDED">Suspendidos</option>
            </select>
          </div>
        </div>

        {/* Lista de Pacientes */}
        <div className="patients-grid">
          {filteredPatients.length === 0 ? (
            <div className="empty-state">
              <Search className="empty-icon" />
              <p className="empty-text">No se encontraron pacientes</p>
              <p className="empty-subtext">Intenta con otros filtros de búsqueda</p>
            </div>
          ) : (
            filteredPatients.map((patient) => (
              <div key={patient.id} className="patient-card">
                <div className="patient-card-header">
                  <div className="patient-avatar">
                    {patient.nombre.charAt(0)}{patient.apellido.charAt(0)}
                  </div>
                  <div className="patient-card-info">
                    <h3 className="patient-card-name">
                      {patient.nombre} {patient.apellido}
                    </h3>
                    <p className="patient-card-email">{patient.email}</p>
                  </div>
                  <span className={`status-badge status-${patient.estadoCuenta.toLowerCase()}`}>
                    {patient.estadoCuenta === 'ACTIVE' ? 'Activo' : 
                     patient.estadoCuenta === 'INACTIVE' ? 'Inactivo' : 'Suspendido'}
                  </span>
                </div>

                <div className="patient-card-body">
                  {patient.enfermedad && (
                    <div className="patient-card-item">
                      <span className="item-label">Enfermedad:</span>
                      <span className="item-value">{patient.enfermedad}</span>
                    </div>
                  )}
                  {patient.telefonoPersonal && (
                    <div className="patient-card-item">
                      <span className="item-label">Teléfono:</span>
                      <span className="item-value">{patient.telefonoPersonal}</span>
                    </div>
                  )}
                  {patient.edad && (
                    <div className="patient-card-item">
                      <span className="item-label">Edad:</span>
                      <span className="item-value">{patient.edad} años</span>
                    </div>
                  )}
                  <div className="patient-card-item">
                    <span className="item-label">Registrado:</span>
                    <span className="item-value">
                      {new Date(patient.registradoEn).toLocaleDateString()}
                    </span>
                  </div>
                </div>

                <div className="patient-card-actions">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => navigate(`/dashboard/patients/${patient.id}`)}
                  >
                    <Eye className="w-4 h-4 mr-1" />
                    Ver Perfil
                  </Button>
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => navigate(`/dashboard/patients/${patient.id}/report`)}
                  >
                    <FileText className="w-4 h-4 mr-1" />
                    Reporte
                  </Button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
      </PageTransition>
    </Layout>
  );
};

export default ViewPatients;
