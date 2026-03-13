import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../../components/common/Layout/Layout';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import PageTransition from '../../components/common/PageTransition';
import { ArrowLeft, Search, Eye, ChevronLeft, ChevronRight } from 'lucide-react';
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
  const [filterDisease, setFilterDisease] = useState<string>('all');
  const [sortBy, setSortBy] = useState<'name' | 'cedula'>('name');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 6;

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
    // Si no hay búsqueda, mostrar todos
    if (searchTerm === '') {
      const matchesDisease = 
        filterDisease === 'all' || 
        (patient.enfermedad && patient.enfermedad === filterDisease);
      return matchesDisease;
    }
    
    // Si hay búsqueda, filtrar
    const searchLower = searchTerm.toLowerCase();
    
    const matchesSearch = 
      (patient.nombre && patient.nombre.toLowerCase().includes(searchLower)) ||
      (patient.apellido && patient.apellido.toLowerCase().includes(searchLower)) ||
      (patient.email && patient.email.toLowerCase().includes(searchLower)) ||
      (patient.cedula && patient.cedula.includes(searchTerm)) ||
      (patient.enfermedad && patient.enfermedad.toLowerCase().includes(searchLower));
    
    const matchesDisease = 
      filterDisease === 'all' || 
      (patient.enfermedad && patient.enfermedad === filterDisease);

    return matchesSearch && matchesDisease;
  }).sort((a, b) => {
    let comparison = 0;
    
    if (sortBy === 'name') {
      const nameA = `${a.nombre || ''} ${a.apellido || ''}`.toLowerCase().trim();
      const nameB = `${b.nombre || ''} ${b.apellido || ''}`.toLowerCase().trim();
      comparison = nameA.localeCompare(nameB);
    } else if (sortBy === 'cedula') {
      const cedulaA = a.cedula || '';
      const cedulaB = b.cedula || '';
      comparison = cedulaA.localeCompare(cedulaB);
    }
    
    return sortOrder === 'asc' ? comparison : -comparison;
  });

  // Paginación
  const totalPages = Math.ceil(filteredPatients.length / ITEMS_PER_PAGE);
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const paginatedPatients = filteredPatients.slice(startIndex, startIndex + ITEMS_PER_PAGE);

  const handleChangePage = (newPage: number) => {
    if (newPage < 1 || newPage > totalPages) return;
    setCurrentPage(newPage);
  };

  // Resetear a página 1 cuando cambian filtros o búsqueda
  React.useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, filterDisease]);

  const handleSearchClick = () => {
    setCurrentPage(1);
    // La búsqueda ya se actualiza automáticamente
  };

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
        {/* Header - Fuera del contenedor principal */}
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
            <ArrowLeft size={20} />
            Volver
          </button>
          <div className="view-patients-header-content">
            <h1 className="view-patients-title">Mis Pacientes</h1>
            <p className="view-patients-subtitle">
              Total: {filteredPatients.length} paciente(s)
            </p>
          </div>
        </div>

        {/* Contenedor principal */}
        <div className="view-patients-container">
          {/* Búsqueda y Filtros */}
          <div className="search-section">
            <div className="search-bar">
              <Search size={20} />
              <input
                type="text"
                placeholder="Buscar pacientes..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <button className="btn-search" onClick={handleSearchClick}>
              Buscar
            </button>
          </div>

          {/* Filtros */}
          <div className="filters-section-patients">
            <div className="filter-group-patients">
              <label className="filter-label-patients">Enfermedad:</label>
              <select
                value={filterDisease}
                onChange={(e) => setFilterDisease(e.target.value)}
                className="filter-select-patients"
              >
                <option value="all">Todas</option>
                <option value="Hipertensión">Hipertensión</option>
                <option value="Diabetes">Diabetes</option>
                <option value="Obesidad">Obesidad</option>
                <option value="Colesterol alto">Colesterol alto</option>
              </select>
            </div>

            <div className="filter-group-patients">
              <label className="filter-label-patients">Ordenar por:</label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as 'name' | 'cedula')}
                className="filter-select-patients"
              >
                <option value="name">Nombre</option>
                <option value="cedula">Cédula</option>
              </select>
            </div>

            <div className="filter-group-patients">
              <label className="filter-label-patients">Orden:</label>
              <select
                value={sortOrder}
                onChange={(e) => setSortOrder(e.target.value as 'asc' | 'desc')}
                className="filter-select-patients"
              >
                <option value="asc">Ascendente</option>
                <option value="desc">Descendente</option>
              </select>
            </div>
          </div>

          {/* Lista de Pacientes */}
          <div className="pacientes-container">
            <div className="patients-grid">
              {paginatedPatients.length === 0 && filteredPatients.length === 0 ? (
                <div className="empty-state">
                  <Search className="empty-icon" />
                  <p className="empty-text">No se encontraron pacientes</p>
                  <p className="empty-subtext">Intenta con otros filtros de búsqueda</p>
                </div>
              ) : (
                paginatedPatients.map((patient) => {
                  const enfermedadColor = patient.enfermedad === 'Hipertensión' ? '#D32F2F' :
                                          patient.enfermedad === 'Diabetes' ? '#FFF176' :
                                          patient.enfermedad === 'Obesidad' ? '#81D4FA' :
                                          patient.enfermedad === 'Colesterol alto' ? '#FFA726' : '#6b7280';
                  
                  return (
                  <div key={patient.id} className="patient-card" style={{ borderLeft: `5px solid ${enfermedadColor}` }}>
                    <div className="patient-card-header">
                      <div className="patient-avatar" style={{ background: enfermedadColor }}>
                        {patient.nombre.charAt(0)}{patient.apellido.charAt(0)}
                      </div>
                      <div className="patient-card-info">
                        <h3 className="patient-card-name">
                          {patient.nombre} {patient.apellido}
                        </h3>
                        {patient.cedula && <p className="patient-card-email">{patient.cedula}</p>}
                      </div>
                    </div>

                    <div className="patient-card-body">
                      {patient.enfermedad && (
                        <div className="patient-card-item">
                          <span className="item-label">Enfermedad:</span>
                          <span className="item-value" style={{ 
                            color: enfermedadColor, 
                            fontWeight: 600
                          }}>{patient.enfermedad}</span>
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
                    </div>
                  </div>
                  );
                })
              )}
            </div>

            {/* Paginación */}
            {totalPages > 1 && (
              <div className="pagination">
                <button
                  className="pagination-btn pagination-arrow"
                  onClick={() => handleChangePage(currentPage - 1)}
                  disabled={currentPage === 1}
                  title="Página anterior"
                >
                  <ChevronLeft size={18} />
                </button>

                <div className="pagination-numbers">
                  {(() => {
                    const pageRange = 4;
                    let startPage, endPage;

                    if (currentPage <= 3) {
                      startPage = 1;
                      endPage = Math.min(pageRange, totalPages);
                    } else {
                      if (currentPage >= totalPages - pageRange + 1) {
                        startPage = Math.max(1, totalPages - pageRange + 1);
                        endPage = totalPages;
                      } else {
                        startPage = currentPage;
                        endPage = Math.min(totalPages, currentPage + pageRange - 1);
                      }
                    }

                    return Array.from({ length: endPage - startPage + 1 }, (_, i) => startPage + i)
                      .map((page) => (
                        <button
                          key={page}
                          className={`pagination-number ${currentPage === page ? 'active' : ''}`}
                          onClick={() => handleChangePage(page)}
                        >
                          {page}
                        </button>
                      ));
                  })()}
                </div>

                <button
                  className="pagination-btn pagination-arrow"
                  onClick={() => handleChangePage(currentPage + 1)}
                  disabled={currentPage === totalPages}
                  title="Página siguiente"
                >
                  <ChevronRight size={18} />
                </button>
              </div>
            )}
          </div>
        </div>
      </PageTransition>
    </Layout>
  );
};

export default ViewPatients;
