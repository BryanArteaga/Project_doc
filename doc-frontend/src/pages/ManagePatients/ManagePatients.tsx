import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../../components/common/Layout/Layout';
import Loading from '../../components/common/Loading';
import PageTransition from '../../components/common/PageTransition';
import { ArrowLeft, Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { SearchFilter, DataGrid, Pagination } from '../../components/shared';
import { doctorService } from '../../services/doctor.service';
import { auditService, AuditAction, AuditLevel } from '../../utils/audit';
import type { Patient } from '../../types';
import './ManagePatients.css';

const ITEMS_PER_PAGE = 12;

const ManagePatients: React.FC = () => {
  const navigate = useNavigate();
  const [patients, setPatients] = useState<Patient[]>([]);
  const [filtrados, setFiltrados] = useState<Patient[]>([]);
  const [loading, setLoading] = useState(true);
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterEnfermedad, setFilterEnfermedad] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  // Obtener enfermedades únicas de los pacientes
  const getUniqueEnfermedades = (): string[] => {
    const enfermedades = new Set(patients
      .map(p => p.enfermedad)
      .filter(Boolean));
    return Array.from(enfermedades).sort();
  };

  // Dark mode detection
  useEffect(() => {
    const checkDarkMode = () => setIsDarkMode(document.documentElement.classList.contains('dark'));
    checkDarkMode();
    const observer = new MutationObserver(checkDarkMode);
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] });
    return () => observer.disconnect();
  }, []);

  // Cargar pacientes
  useEffect(() => {
    auditService.log(
      AuditAction.VIEW_PATIENTS,
      AuditLevel.INFO,
      'Accedió a la página de gestión de pacientes'
    );
    loadPatients();
  }, []);

  // Aplicar filtros
  useEffect(() => {
    aplicarFiltros();
  }, [patients, searchTerm, filterEnfermedad]);

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

  const aplicarFiltros = () => {
    let resultado = [...patients];

    if (searchTerm.trim()) {
      const term = searchTerm.toLowerCase();
      resultado = resultado.filter(p =>
        p.nombre.toLowerCase().includes(term) ||
        p.apellido.toLowerCase().includes(term) ||
        p.email.toLowerCase().includes(term)
      );
    }

    if (filterEnfermedad) {
      resultado = resultado.filter(p => p.enfermedad === filterEnfermedad);
    }

    // Calcular paginación
    const total = Math.ceil(resultado.length / ITEMS_PER_PAGE);
    setTotalPages(total || 1);
    setCurrentPage(1);
    setFiltrados(resultado);
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setFilterEnfermedad('');
  };

  if (loading) {
    return (
      <Layout>
        <Loading message="Cargando pacientes..." fullHeight />
      </Layout>
    );
  }

  // Paginación
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const endIndex = startIndex + ITEMS_PER_PAGE;
  const paginatedPatients = filtrados.slice(startIndex, endIndex);

  const dataGridItems = paginatedPatients.map(patient => ({
    id: patient.id,
    title: `${patient.nombre} ${patient.apellido}`,
    description: patient.email,
    badges: [
      ...(patient.enfermedad ? [{ label: patient.enfermedad, color: 'info' as const }] : []),
      { label: `ID: ${patient.id}`, color: 'default' as const }
    ],
    onClick: () => {} // No hay acción de click
  }));

  return (
    <Layout>
      <PageTransition>
        <div className="manage-patients-container">
          {/* Header */}
          <div className="manage-patients-header">
            <div className="header-top">
              <button onClick={() => navigate('/admin')} className="back-button">
                <ArrowLeft size={20} />
                Volver
              </button>
              <h1 className="manage-patients-title">Gestionar Pacientes</h1>
              <div style={{ width: '120px' }} /> {/* Spacer para centrar título */}
            </div>
            <p className="manage-patients-subtitle">
              Total: {patients.length} pacientes
            </p>

            {/* Search & Filter */}
            <SearchFilter
              searchValue={searchTerm}
              onSearchChange={setSearchTerm}
              onSearchSubmit={() => setCurrentPage(1)}
              filterOptions={[
                {
                  name: 'enfermedad',
                  label: 'Enfermedad',
                  type: 'select',
                  options: getUniqueEnfermedades().map(e => ({ label: e, value: e })),
                  value: filterEnfermedad
                }
              ]}
              onFilterChange={(name, value) => {
                if (name === 'enfermedad') setFilterEnfermedad(value as string);
              }}
              onApplyFilters={() => setCurrentPage(1)}
              onClearFilters={handleClearFilters}
              isDarkMode={isDarkMode}
              placeholder="Buscar pacientes por nombre, apellido o email..."
            />
          </div>

          {/* Data Grid */}
          <DataGrid
            items={dataGridItems}
            isDarkMode={isDarkMode}
            columns={3}
            emptyMessage="No hay pacientes disponibles"
          />

          {/* Pagination */}
          {totalPages > 1 && (
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
              itemsPerPage={ITEMS_PER_PAGE}
              totalItems={filtrados.length}
              isDarkMode={isDarkMode}
            />
          )}
        </div>
      </PageTransition>
    </Layout>
  );
};

export default ManagePatients;
