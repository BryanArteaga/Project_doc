import React, { useEffect, useState } from 'react';
import { Plus, ArrowLeft, Lightbulb } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import Layout from '../../components/common/Layout/Layout';
import Loading from '../../components/common/Loading';
import PageTransition from '../../components/common/PageTransition';
import { CRUDModal, ConfirmModal, SearchFilter, DataGrid, Pagination } from '../../components/shared';
import { consejoService, type Consejo, type CreateConsejoRequest, type Enfermedad } from '../../services/consejo.service';
import './GestionConsejos.css';

const GestionConsejos: React.FC = () => {
  const navigate = useNavigate();
  const [consejos, setConsejos] = useState<Consejo[]>([]);
  const [filtrados, setFiltrados] = useState<Consejo[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [showConfirmSave, setShowConfirmSave] = useState(false);
  const [showConfirmCancel, setShowConfirmCancel] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [searchLoading, setSearchLoading] = useState(false);

  // Search & Filter states - Input en vivo
  const [searchInput, setSearchInput] = useState('');
  const [searchTerm, setSearchTerm] = useState('');   // búsqueda aplicada (on-demand)
  
  // Filtro pendiente (siendo editado)
  const [filterEnfermedad_temp, setFilterEnfermedad_temp] = useState<Enfermedad | ''>('');
  // Filtro aplicado
  const [filterEnfermedad, setFilterEnfermedad] = useState<Enfermedad | ''>('');
  
  // ✅ Ordenamiento - Input en vivo
  const [sortBy_temp, setSortBy_temp] = useState<'az' | 'za' | 'reciente' | 'antiguo' | ''>('');
  // ✅ Ordenamiento - Aplicado
  const [sortBy, setSortBy] = useState<'az' | 'za' | 'reciente' | 'antiguo' | ''>('');

  // Form states
  const [formData, setFormData] = useState<CreateConsejoRequest>({
    consejo: '',
    enfermedad: ''
  });
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [originalFormData, setOriginalFormData] = useState<CreateConsejoRequest | null>(null);

  // Pagination states
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const ITEMS_PER_PAGE = 12;

  const ENFERMEDADES: Enfermedad[] = ['Hipertensión', 'Diabetes', 'Obesidad', 'Colesterol alto'];

  // ============ HELPER FUNCTIONS ============
  const getEnfermedadColor = (enfermedad?: string): string => {
    switch (enfermedad) {
      case 'Hipertensión': return '#B32428';
      case 'Diabetes': return '#E5BE01';
      case 'Obesidad': return '#2595AB';
      case 'Colesterol alto': return '#FF8000';
      default: return '#6b7280';
    }
  };

  // Detectar modo oscuro
  useEffect(() => {
    const checkDarkMode = () => {
      setIsDarkMode(document.documentElement.classList.contains('dark'));
    };
    checkDarkMode();

    const observer = new MutationObserver(checkDarkMode);
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class']
    });

    return () => observer.disconnect();
  }, []);

  // Cargar consejos cuando cambia el filtro de enfermedad aplicado
  useEffect(() => {
    loadConsejos();
  }, [filterEnfermedad]);

  // Aplicar filtros de búsqueda cuando cambian los consejos o el término de búsqueda
  useEffect(() => {
    aplicarFiltros();
  }, [consejos, searchTerm, sortBy]);

  const loadConsejos = async () => {
    try {
      setLoading(true);
      const datos = await consejoService.listarConsejos({
        page: 1,
        limit: 9999,
        enfermedad: filterEnfermedad || undefined
      });

      setConsejos(datos.consejos);
      setCurrentPage(1);
    } catch (error: any) {
      console.error('Error al cargar consejos:', error);
      toast.error('Error al cargar consejos');
    } finally {
      setLoading(false);
    }
  };

  const aplicarFiltros = () => {
    let resultado = [...consejos];

    if (searchTerm.trim()) {
      const term = searchTerm.toLowerCase();
      resultado = resultado.filter(c => 
        c.consejo.toLowerCase().includes(term)
      );
    }

    // ✅ Aplicar ordenamiento
    if (sortBy === 'az') {
      resultado.sort((a, b) => a.consejo.localeCompare(b.consejo, 'es'));
    } else if (sortBy === 'za') {
      resultado.sort((a, b) => b.consejo.localeCompare(a.consejo, 'es'));
    } else if (sortBy === 'reciente') {
      resultado.sort((a, b) => {
        const dateA = a.creadoEn ? new Date(a.creadoEn).getTime() : 0;
        const dateB = b.creadoEn ? new Date(b.creadoEn).getTime() : 0;
        return dateB - dateA;
      });
    } else if (sortBy === 'antiguo') {
      resultado.sort((a, b) => {
        const dateA = a.creadoEn ? new Date(a.creadoEn).getTime() : 0;
        const dateB = b.creadoEn ? new Date(b.creadoEn).getTime() : 0;
        return dateA - dateB;
      });
    }

    setFiltrados(resultado);
    setTotalPages(Math.ceil(resultado.length / ITEMS_PER_PAGE));
  };

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.consejo.trim()) {
      errors.consejo = 'El consejo es requerido';
    } else if (formData.consejo.trim().length < 10) {
      errors.consejo = 'El consejo debe tener al menos 10 caracteres';
    }

    if (!formData.enfermedad) {
      errors.enfermedad = 'La enfermedad es requerida';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      toast.error('Por favor completa todos los campos requeridos');
      return;
    }

    if (editingId) {
      setShowConfirmSave(true);
      return;
    }

    await guardarConsejo();
  };

  const guardarConsejo = async () => {
    if (!validateForm()) {
      return;
    }

    try {
      setIsSubmitting(true);
      const dataToSend = {
        consejo: formData.consejo.trim(),
        enfermedad: formData.enfermedad
      };

      if (editingId) {
        await consejoService.actualizarConsejo(editingId, dataToSend);
        toast.success('Consejo actualizado');
      } else {
        await consejoService.crearConsejo(dataToSend);
        toast.success('Consejo creado');
      }

      loadConsejos();
      setShowModal(false);
      setShowConfirmSave(false);
      setEditingId(null);
      resetForm();
    } catch (error: any) {
      if ((error as any).isDuplicate) {
        toast.error(error.message);
      } else {
        toast.error(error.response?.data?.error || 'Error al guardar consejo');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEdit = (consejo: Consejo) => {
    setFormData({
      consejo: consejo.consejo,
      enfermedad: consejo.enfermedad
    });
    setOriginalFormData({
      consejo: consejo.consejo,
      enfermedad: consejo.enfermedad
    });
    setEditingId(consejo.id);
    setShowModal(true);
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('¿Estás seguro de que deseas eliminar este consejo?')) {
      try {
        await consejoService.eliminarConsejo(id);
        toast.success('Consejo eliminado');
        loadConsejos();
      } catch (error: any) {
        toast.error('Error al eliminar consejo');
      }
    }
  };

  const resetForm = () => {
    setFormData({
      consejo: '',
      enfermedad: ''
    });
    setFormErrors({});
    setOriginalFormData(null);
  };

  const handleCloseModal = () => {
    if (JSON.stringify(formData) !== JSON.stringify(originalFormData)) {
      setShowConfirmCancel(true);
    } else {
      setShowModal(false);
      setEditingId(null);
      resetForm();
    }
  };

  const handleSearch = () => {
    setSearchLoading(true);
    setSearchTerm(searchInput);
    setCurrentPage(1);
    setTimeout(() => setSearchLoading(false), 300);
  };

  const handleApplyFilters = () => {
    setFilterEnfermedad(filterEnfermedad_temp);
    setSortBy(sortBy_temp);
    setCurrentPage(1);
  };

  const handleClearFilters = () => {
    setSearchInput('');
    setSearchTerm('');
    setFilterEnfermedad_temp('');
    setFilterEnfermedad('');
    setSortBy_temp('');
    setSortBy('');
    setCurrentPage(1);
  };

  if (loading || searchLoading) {
    return (
      <Layout>
        <Loading message="Cargando consejos..." fullHeight />
      </Layout>
    );
  }

  // Calcular paginación
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const paginatedConsejos = filtrados.slice(startIndex, startIndex + ITEMS_PER_PAGE);

  const dataGridItems = paginatedConsejos.map(consejo => {
    const enfermedadColor = getEnfermedadColor(consejo.enfermedad);
    return {
      id: consejo.id,
      title: consejo.consejo,
      badges: [
        {
          label: consejo.enfermedad,
          backgroundColor: enfermedadColor,
          textColor: '#ffffff'
        }
      ],
      borderLeftColor: enfermedadColor,
      onEdit: () => handleEdit(consejo),
      onDelete: () => handleDelete(consejo.id)
    };
  });

  return (
    <Layout>
      <PageTransition>
        <div className="gestion-consejos-page">
          {/* Header */}
          <div className="gestion-header">
            <div className="header-top">
              <button 
                onClick={() => navigate('/dashboard')}
                className="back-button"
              >
                <ArrowLeft size={20} />
                Volver
              </button>
              <h1 className="gestion-title">Gestión de Consejos</h1>
              <button
                onClick={() => {
                  resetForm();
                  setEditingId(null);
                  setOriginalFormData({ consejo: '', enfermedad: '' });
                  setShowModal(true);
                }}
                className="btn-primary"
              >
                <Plus size={18} />
                Nuevo Consejo
              </button>
            </div>

            {/* Search & Filter */}
            <SearchFilter
              searchValue={searchInput}
              onSearchChange={setSearchInput}
              onSearchSubmit={handleSearch}
              filterOptions={[
                {
                  name: 'enfermedad',
                  label: 'Enfermedad',
                  type: 'select',
                  options: ENFERMEDADES.map(e => ({ label: e, value: e })),
                  value: filterEnfermedad_temp
                },
                {
                  name: 'sortBy',
                  label: 'Ordenar',
                  type: 'select',
                  options: [
                    { label: 'A - Z', value: 'az' },
                    { label: 'Z - A', value: 'za' },
                    { label: 'Más reciente', value: 'reciente' },
                    { label: 'Más antiguo', value: 'antiguo' }
                  ],
                  value: sortBy_temp
                }
              ]}
              onFilterChange={(name, value) => {
                if (name === 'enfermedad') setFilterEnfermedad_temp(value as Enfermedad | '');
                if (name === 'sortBy') setSortBy_temp(value as 'az' | 'za' | 'reciente' | 'antiguo' | '');
              }}
              onApplyFilters={handleApplyFilters}
              onClearFilters={handleClearFilters}
              isDarkMode={isDarkMode}
              placeholder="Buscar consejos..."
            />
          </div>

          {/* Data Grid */}
          <div className="consejos-section">
            {filtrados.length === 0 ? (
              <div className="empty-state">
                <Lightbulb size={48} className="empty-icon" />
                <p>No hay consejos disponibles</p>
              </div>
            ) : (
              <DataGrid
                items={dataGridItems}
                isDarkMode={isDarkMode}
                columns={3}
                emptyMessage="No hay consejos"
              />
            )}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={(page) => setCurrentPage(page)}
              itemsPerPage={ITEMS_PER_PAGE}
              totalItems={filtrados.length}
              isDarkMode={isDarkMode}
            />
          )}

          {/* CRUD Modal */}
          <CRUDModal
            isOpen={showModal}
            title={editingId ? 'Editar Consejo' : 'Nuevo Consejo'}
            onSubmit={handleSubmit}
            onCancel={handleCloseModal}
            onClose={handleCloseModal}
            errors={formErrors}
            isLoading={isSubmitting}
            submitLabel={editingId ? 'Actualizar' : 'Crear'}
            cancelLabel="Cancelar"
            isDarkMode={isDarkMode}
          >
            {/* Enfermedad Select */}
            <div className="form-group">
              <label htmlFor="enfermedad">Enfermedad *</label>
              <select
                id="enfermedad"
                value={formData.enfermedad}
                onChange={(e) => setFormData({ ...formData, enfermedad: e.target.value as Enfermedad })}
                className="form-input"
              >
                <option value="">Selecciona una enfermedad</option>
                {ENFERMEDADES.map(e => (
                  <option key={e} value={e}>{e}</option>
                ))}
              </select>
              {formErrors.enfermedad && (
                <span className="error-message">{formErrors.enfermedad}</span>
              )}
            </div>

            {/* Consejo Textarea */}
            <div className="form-group">
              <label htmlFor="consejo">Consejo *</label>
              <textarea
                id="consejo"
                value={formData.consejo}
                onChange={(e) => setFormData({ ...formData, consejo: e.target.value })}
                className={`form-textarea ${formErrors.consejo ? 'error' : ''}`}
                placeholder="Ingresa el consejo para los pacientes..."
                rows={4}
              />
              {formErrors.consejo && (
                <span className="error-message">{formErrors.consejo}</span>
              )}
              <div className="char-count">
                {formData.consejo.length} / 500 caracteres
              </div>
            </div>
          </CRUDModal>

          {/* Confirm Save Modal */}
          <ConfirmModal
            isOpen={showConfirmSave}
            title="¿Confirmar cambios?"
            message="¿Estás seguro de que deseas actualizar este consejo?"
            onConfirm={guardarConsejo}
            onCancel={() => setShowConfirmSave(false)}
            confirmLabel="Confirmar"
            cancelLabel="Cancelar"
            isDarkMode={isDarkMode}
            isLoading={isSubmitting}
          />

          {/* Confirm Cancel Modal */}
          <ConfirmModal
            isOpen={showConfirmCancel}
            title="¿Descartar cambios?"
            message="Tienes cambios sin guardar. ¿Deseas descartarlos?"
            onConfirm={() => {
              setShowConfirmCancel(false);
              setShowModal(false);
              setEditingId(null);
              resetForm();
            }}
            onCancel={() => setShowConfirmCancel(false)}
            confirmLabel="Descartar"
            cancelLabel="Continuar editando"
            isDarkMode={isDarkMode}
          />
        </div>
      </PageTransition>
    </Layout>
  );
};

export default GestionConsejos;
