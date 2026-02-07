import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../../components/common/Layout/Layout';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import { ArrowLeft, Search, UserCheck, UserX } from 'lucide-react';
import toast from 'react-hot-toast';
import { doctorService } from '../../services/doctor.service';
import { auditService, AuditAction, AuditLevel } from '../../utils/audit';
import type { Patient } from '../../types';
import './ManagePatients.css';

const ManagePatients: React.FC = () => {
  const navigate = useNavigate();
  const [patients, setPatients] = useState<Patient[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    auditService.log(
      AuditAction.VIEW_PATIENTS,
      AuditLevel.INFO,
      'Accedió a la página de gestión de pacientes'
    );
    loadPatients();
  }, []);

  const loadPatients = async () => {
    try {
      setLoading(true);
      const data = await doctorService.getActivePatients();
      setPatients(data);
    } catch (error: any) {
      console.error('Error al cargar pacientes:', error);
      
      if (error.response?.status === 401) {
        toast.error('Sesión expirada. Por favor inicia sesión nuevamente.');
      } else {
        toast.error('Error al cargar pacientes');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleActivate = async (id: number) => {
    try {
      const doctorData = JSON.parse(localStorage.getItem('doctor_data') || '{}');
      await doctorService.activatePatient(id, doctorData.id);
      
      auditService.log(
        AuditAction.ACTIVATE_PATIENT,
        AuditLevel.INFO,
        `Paciente activado (ID: ${id})`,
        { patientId: id, doctorId: doctorData.id }
      );
      
      toast.success('Paciente activado correctamente');
      loadPatients();
    } catch (error) {
      auditService.log(
        AuditAction.ERROR,
        AuditLevel.ERROR,
        'Error al activar paciente',
        { patientId: id, error }
      );
      toast.error('Error al activar paciente');
    }
  };

  const filteredPatients = patients.filter(patient =>
    patient.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
    patient.apellido.toLowerCase().includes(searchTerm.toLowerCase()) ||
    patient.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <Layout>
        <Loading message="Cargando pacientes..." fullHeight />
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="manage-patients-container">
        <div className="manage-patients-header">
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
          <h1 className="manage-patients-title">Gestionar Pacientes</h1>
          <p className="manage-patients-subtitle">
            Total: {patients.length} | Activos: {patients.filter(p => p.estadoCuenta === 'ACTIVE').length} | Pendientes: {patients.filter(p => p.estadoCuenta === 'INACTIVE').length}
          </p>
        </div>

        {/* Barra de búsqueda */}
        <div className="search-container">
          <Search className="search-icon" />
          <input
            type="text"
            placeholder="Buscar por nombre, apellido o email..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-input"
          />
        </div>

        {/* Lista de pacientes */}
        <div className="patients-grid">
          {filteredPatients.length === 0 ? (
            <div className="empty-state">
              <p>No se encontraron pacientes</p>
            </div>
          ) : (
            filteredPatients.map((patient) => (
              <div key={patient.id} className="patient-card">
                <div className="patient-card-header">
                  <div className="patient-avatar">
                    {patient.nombre.charAt(0)}{patient.apellido.charAt(0)}
                  </div>
                  <div className="patient-info">
                    <h3 className="patient-name">{patient.nombre} {patient.apellido}</h3>
                    <p className="patient-email">{patient.email}</p>
                    {patient.enfermedad && (
                      <p className="patient-disease">🏥 {patient.enfermedad}</p>
                    )}
                  </div>
                </div>
                
                <div className="patient-card-footer">
                  <span className={`patient-status ${patient.estadoCuenta === 'ACTIVE' ? 'active' : 'inactive'}`}>
                    {patient.estadoCuenta === 'ACTIVE' ? (
                      <>
                        <UserCheck className="w-4 h-4" />
                        Activo
                      </>
                    ) : (
                      <>
                        <UserX className="w-4 h-4" />
                        Pendiente
                      </>
                    )}
                  </span>
                  {patient.estadoCuenta === 'INACTIVE' && (
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => handleActivate(patient.id)}
                    >
                      Activar
                    </Button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </Layout>
  );
};

export default ManagePatients;
