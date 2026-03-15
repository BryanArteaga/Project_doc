import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../../components/common/Layout/Layout';
import Loading from '../../components/common/Loading';
import PageTransition from '../../components/common/PageTransition';
import { ArrowLeft, Eye } from 'lucide-react';
import toast from 'react-hot-toast';
import { SearchFilter } from '../../components/shared/SearchFilter';
import { DataGrid, DataGridCardProps } from '../../components/shared/DataGrid';
import { Pagination } from '../../components/shared/Pagination';
import { doctorService } from '../../services/doctor.service';
import type { Patient } from '../../types';
import './ViewPatients.css';

const ViewPatients: React.FC = () => {
  const navigate = useNavigate();
  const [patients, setPatients] = useState<Patient[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchLoading, setSearchLoading] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [searchInput, setSearchInput] = useState(''); // input en vivo
  const [searchTerm, setSearchTerm] = useState('');   // búsqueda aplicada (on-demand)
  
  // Filtros pendientes (siendo editados)
  const [filterEnfermedad_temp, setFilterEnfermedad_temp] = useState('');
  const [sortBy_temp, setSortBy_temp] = useState('name');
  const [sortOrder_temp, setSortOrder_temp] = useState('asc');
  
  // Filtros aplicados
  const [filterEnfermedad, setFilterEnfermedad] = useState('');
  const [sortBy, setSortBy] = useState('name');
  const [sortOrder, setSortOrder] = useState('asc');
  
  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 6;

  useEffect(() => {
    loadPatients();
    // Detectar dark mode
    const html = document.documentElement;
    setIsDarkMode(html.classList.contains('dark') || html.classList.contains('dark-mode'));
    
    const observer = new MutationObserver(() => {
      setIsDarkMode(html.classList.contains('dark') || html.classList.contains('dark-mode'));
    });
    observer.observe(html, { attributes: true, attributeFilter: ['class'] });
    return () => observer.disconnect();
  }, []);

  // Resetear página cuando cambia búsqueda o filtros
  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, filterEnfermedad]);

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

  const handleSearch = async () => {
    setSearchLoading(true);
    try {
      setSearchTerm(searchInput);
      await new Promise(resolve => setTimeout(resolve, 300));
    } finally {
      setSearchLoading(false);
    }
  };

  const handleApplyFilters = async () => {
    setSearchLoading(true);
    try {
      setFilterEnfermedad(filterEnfermedad_temp);
      setSortBy(sortBy_temp);
      setSortOrder(sortOrder_temp);
      setCurrentPage(1);
      await new Promise(resolve => setTimeout(resolve, 300));
    } finally {
      setSearchLoading(false);
    }
  };

  const handleClearFilters = async () => {
    setSearchLoading(true);
    try {
      setFilterEnfermedad_temp('');
      setSortBy_temp('name');
      setSortOrder_temp('asc');
      setFilterEnfermedad('');
      setSortBy('name');
      setSortOrder('asc');
      setSearchInput('');
      setSearchTerm('');
      setCurrentPage(1);
      await new Promise(resolve => setTimeout(resolve, 300));
    } finally {
      setSearchLoading(false);
    }
  };

  const filteredPatients = patients
    .filter(patient => {
      const searchLower = searchTerm.toLowerCase();
      const matchesSearch = 
        (patient.nombre && patient.nombre.toLowerCase().includes(searchLower)) ||
        (patient.apellido && patient.apellido.toLowerCase().includes(searchLower)) ||
        (patient.email && patient.email.toLowerCase().includes(searchLower)) ||
        (patient.cedula && patient.cedula.includes(searchTerm)) ||
        (patient.enfermedad && patient.enfermedad.toLowerCase().includes(searchLower));
      
      const matchesDisease = 
        !filterEnfermedad || 
        (patient.enfermedad && patient.enfermedad === filterEnfermedad);

      return matchesSearch && matchesDisease;
    })
    .sort((a, b) => {
      let comparison = 0;
      
      if (sortBy === 'name') {
        const nameA = `${a.nombre || ''} ${a.apellido || ''}`.toLowerCase().trim();
        const nameB = `${b.nombre || ''} ${b.apellido || ''}`.toLowerCase().trim();
        comparison = nameA.localeCompare(nameB);
      } else if (sortBy === 'cedula') {
        comparison = (a.cedula ?? '').localeCompare(b.cedula ?? '');
      }
      
      return sortOrder === 'asc' ? comparison : -comparison;
    });

  const totalPages = Math.ceil(filteredPatients.length / ITEMS_PER_PAGE);
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const paginatedPatients = filteredPatients.slice(startIndex, startIndex + ITEMS_PER_PAGE);

  // Preparar datos para DataGrid
  const dataGridItems: DataGridCardProps[] = paginatedPatients.map(patient => {
    const enfermedadColor = 
      patient.enfermedad === 'Hipertensión' ? '#B32428' :
      patient.enfermedad === 'Diabetes' ? '#E5BE01' :
      patient.enfermedad === 'Obesidad' ? '#2595AB' :
      patient.enfermedad === 'Colesterol alto' ? '#FF8000' : '#6b7280';

    return {
      id: patient.id,
      title: `${patient.nombre} ${patient.apellido}`,
      description: patient.cedula || '',
      badges: patient.enfermedad ? [
        {
          label: patient.enfermedad,
          backgroundColor: enfermedadColor,
          textColor: '#ffffff',
          className: `badge-enfermedad`,
        }
      ] : [],
      borderLeftColor: enfermedadColor,
      customContent: (
        <div className="patient-info">
          <div className="info-row">
            <span className="info-label">Edad:</span>
            <span className="info-value">{patient.edad || '-'} años</span>
          </div>
          <div className="info-row">
            <span className="info-label">Registrado:</span>
            <span className="info-value">{new Date(patient.registradoEn).toLocaleDateString()}</span>
          </div>
          <button
            className="btn-ver-perfil"
            onClick={(e) => {
              e.stopPropagation();
              navigate(`/dashboard/patients/${patient.id}`);
            }}
          >
            Ver Perfil
          </button>
        </div>
      ),
    };
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
          {/* Header */}
          <div className="view-patients-header">
            <button
              onClick={() => navigate('/dashboard')}
              className="back-button"
            >
              <ArrowLeft size={20} />
              Volver
            </button>
            <div className="header-content">
              <h1 className="view-patients-title">Mis Pacientes</h1>
              <p className="view-patients-subtitle">
                Total: {filteredPatients.length} paciente(s)
              </p>
            </div>
          </div>

          {/* Search & Filters */}
          <SearchFilter
            searchValue={searchInput}
            onSearchChange={setSearchInput}
            onSearchSubmit={handleSearch}
            placeholder="Buscar pacientes..."
            filterOptions={[
              {
                name: 'enfermedad',
                label: 'Enfermedad',
                type: 'select',
                value: filterEnfermedad_temp,
                options: [
                  { label: 'Hipertensión', value: 'Hipertensión' },
                  { label: 'Diabetes', value: 'Diabetes' },
                  { label: 'Obesidad', value: 'Obesidad' },
                  { label: 'Colesterol alto', value: 'Colesterol alto' },
                ]
              },
              {
                name: 'sortBy',
                label: 'Ordenar por',
                type: 'select',
                value: sortBy_temp,
                options: [
                  { label: 'Nombre', value: 'name' },
                  { label: 'Cédula', value: 'cedula' }
                ]
              },
              {
                name: 'sortOrder',
                label: 'Orden',
                type: 'select',
                value: sortOrder_temp,
                options: [
                  { label: 'Ascendente', value: 'asc' },
                  { label: 'Descendente', value: 'desc' }
                ]
              }
            ]}
            onFilterChange={(filterName, value) => {
              if (filterName === 'enfermedad') setFilterEnfermedad_temp(value);
              if (filterName === 'sortBy') setSortBy_temp(value);
              if (filterName === 'sortOrder') setSortOrder_temp(value as 'asc' | 'desc');
            }}
            onApplyFilters={handleApplyFilters}
            onClearFilters={handleClearFilters}
            isDarkMode={isDarkMode}
          />

          {searchLoading && (
            <div className="search-loading">
              <div className="loading-spinner" />
              <span>Buscando pacientes...</span>
            </div>
          )}

          {/* Patient Grid */}
          <DataGrid
            items={dataGridItems}
            columns={2}
            emptyMessage="No se encontraron pacientes"
            isDarkMode={isDarkMode}
          />

          {/* Pagination */}
          {totalPages > 1 && (
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
              isDarkMode={isDarkMode}
            />
          )}
        </div>
      </PageTransition>
    </Layout>
  );
};

export default ViewPatients;
