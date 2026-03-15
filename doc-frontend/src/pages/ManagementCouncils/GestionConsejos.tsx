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

  // Search & Filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [filterEnfermedad, setFilterEnfermedad] = useState<Enfermedad | ''>('');

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

  useEffect(() => {
    loadConsejos(1);
  }, [filterEnfermedad]);

  useEffect(() => {
    aplicarFiltros();
  }, [consejos, searchTerm]);

  const loadConsejos = async (page: number = 1) => {
    try {
      setLoading(true);
      const limit = searchTerm ? 9999 : ITEMS_PER_PAGE;
      const actualPage = searchTerm ? 1 : page;

      const datos = await consejoService.listarConsejos({
        page: actualPage,
        limit,
        enfermedad: filterEnfermedad || undefined
      });

      setConsejos(datos.consejos);
      setTotalPages(searchTerm ? 1 : datos.paginacion.totalPaginas);
      setCurrentPage(actualPage);
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

    setFiltrados(resultado);
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

      loadConsejos(1);
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
        loadConsejos(1);
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

  const handleClearFilters = () => {
    setSearchTerm('');
    setFilterEnfermedad('');
  };

  if (loading) {
    return (
      <Layout>
        <Loading message="Cargando consejos..." fullHeight />
      </Layout>
    );
  }

  const dataGridItems = filtrados.map(consejo => {
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
              searchValue={searchTerm}
              onSearchChange={setSearchTerm}
              onSearchSubmit={() => loadConsejos(1)}
              filterOptions={[
                {
                  name: 'enfermedad',
                  label: 'Enfermedad',
                  type: 'select',
                  options: ENFERMEDADES.map(e => ({ label: e, value: e })),
                  value: filterEnfermedad
                }
              ]}
              onFilterChange={(name, value) => setFilterEnfermedad(value as Enfermedad | '')}
              onApplyFilters={() => loadConsejos(1)}
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
              onPageChange={(page) => loadConsejos(page)}
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
                disabled={!!editingId}
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
